package com.atlassian.plugin.scripting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.IOUtils;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformer;
import com.atlassian.plugin.osgi.factory.transform.DefaultPluginTransformer;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.stage.*;
import com.atlassian.plugin.osgi.factory.OsgiPluginXmlDescriptorParserFactory;
import com.atlassian.plugin.osgi.factory.UnrecognisedModuleDescriptorFallbackFactory;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.XmlDescriptorParser;
import com.atlassian.plugin.*;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;
import com.atlassian.plugin.util.resource.AlternativeDirectoryResourceLoader;
import com.atlassian.plugin.scripting.stages.ConventionsToDescriptorStage;
import com.atlassian.plugin.scripting.stages.ComponentsToServicesStage;
import com.atlassian.plugin.scripting.variables.JsScript;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.descriptors.ChainModuleDescriptorFactory;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 13, 2008
 * Time: 1:50:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptingOsgiPluginFactory implements PluginFactory
{
    private static final Log log = LogFactory.getLog(ScriptingOsgiPluginFactory.class);

    private final OsgiContainerManager osgi;
    private final PluginTransformer pluginTransformer;
    private final String pluginDescriptorFileName;
    private final DescriptorParserFactory descriptorParserFactory;
    private final Map<Long,ScriptManager> scriptManagers;
    private final Map<Long,List<BundleActivator>> bundleActivators;
    private boolean listenerAdded = false;

    private ServiceTracker moduleDescriptorFactoryTracker;
    private final AlternativeDirectoryResourceLoader altResourceLoader;

    public ScriptingOsgiPluginFactory(String pluginDescriptorFileName, OsgiContainerManager osgi)
    {
        Validate.notNull(pluginDescriptorFileName, "Plugin descriptor is required");
        Validate.notNull(osgi, "The OSGi container is required");

        pluginTransformer = new DefaultPluginTransformer(pluginDescriptorFileName, Arrays.asList(
            new ConventionsToDescriptorStage(),
            new AddBundleOverridesStage(),
            // new ComponentImportSpringStage(),
            new ComponentsToServicesStage(),
            // new ModuleTypeSpringStage(),
            new GenerateManifestStage()
        ));
        this.osgi = osgi;
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new OsgiPluginXmlDescriptorParserFactory();
        this.scriptManagers = CopyOnWriteMap.newHashMap();
        this.bundleActivators = CopyOnWriteMap.newHashMap();
        this.altResourceLoader = new AlternativeDirectoryResourceLoader();
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");

        String pluginKey = null;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);

            if (descriptorStream != null)
            {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
                if (descriptorParser.getPluginsVersion() == 3)
                    pluginKey = descriptorParser.getKey();
                else
                    return null;
            }
            else
            {
                IOUtils.closeQuietly(descriptorStream);
                descriptorStream = pluginArtifact.getResourceAsStream("atlassian-plugin.js");

                if (descriptorStream != null)
                {
                    ScriptManager mgr = new ScriptManager(!altResourceLoader.getResourceDirectories().isEmpty());
                    JsScript script = mgr.run("atlassian-plugin.js", descriptorStream, Collections.<String, Object>emptyMap());
                    final DescriptorParser descriptorParser = new XmlDescriptorParser(script.getConfig());
                    if (descriptorParser.getPluginsVersion() == 3)
                        pluginKey = descriptorParser.getKey();
                    else
                        return null;
                }
            }
        }
        catch (IOException e)
        {
            throw new PluginParseException("Unable to parse atlassian-plugin.properties", e);
        }
        finally
        {
            IOUtils.closeQuietly(descriptorStream);
        }
        return pluginKey;
    }

    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Validate.notNull(deploymentUnit, "The plugin deployment unit is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");

        if (!listenerAdded)
        {
            osgi.getBundles()[0].getBundleContext().addBundleListener(new BundleListener()
            {
                public void bundleChanged(BundleEvent event)
                {
                    final long bundleId = event.getBundle().getBundleId();
                    if (event.getType() == BundleEvent.STARTED)
                    {
                        if (scriptManagers.containsKey(bundleId))
                        {

                            try
                            {
                                List<BundleActivator> activators = bundleActivators.get(bundleId);
                                for (BundleActivator activator : activators)
                                {
                                    activator.start(event.getBundle().getBundleContext());
                                }
                            }
                            catch (Exception e)
                            {
                                throw new PluginException(e);
                            }
                        }
                    }
                    else if (event.getType() == BundleEvent.STOPPED)
                    {
                        if (bundleActivators.containsKey(bundleId))
                        {

                            try
                            {
                                List<BundleActivator> activators = bundleActivators.get(bundleId);
                                if (activators != null)
                                {
                                    for (BundleActivator activator : activators)
                                    {
                                        activator.stop(event.getBundle().getBundleContext());
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                throw new PluginException(e);
                            }
                        }
                    }

                }
            });
            listenerAdded = true;
        }
        Plugin plugin = null;
        File transformedFile = null;
        try
        {
            ScriptManager scriptManager = new ScriptManager(!altResourceLoader.getResourceDirectories().isEmpty());
            ScriptingTransformContext context = new ScriptingTransformContext(osgi.getHostComponentRegistrations(), deploymentUnit.getPath(), pluginDescriptorFileName, scriptManager);
            transformedFile = pluginTransformer.transform(context);
            Bundle bundle = osgi.installBundle(transformedFile);
            bundleActivators.put(bundle.getBundleId(), context.getBundleActivators());
            scriptManagers.put(bundle.getBundleId(), scriptManager);
            plugin = new ScriptingOsgiPlugin(bundle, scriptManager);
        } catch (OsgiContainerException e)
        {
            return reportUnloadablePlugin(transformedFile, e);
        } catch (PluginTransformationException ex)
        {
            return reportUnloadablePlugin(transformedFile, ex);
        }


        InputStream pluginDescriptor = null;
        PluginClassLoader loader = new PluginClassLoader(transformedFile);

        try
        {
            ModuleDescriptorFactory combinedFactory = getChainedModuleDescriptorFactory(moduleDescriptorFactory);
            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);

            plugin = parser.configurePlugin(combinedFactory, plugin);
        }
        finally
        {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }

    /**
     * Get a chained module descriptor factory that includes any dynamically available descriptor factories
     *
     * @param originalFactory The factory provided by the host application
     * @return The composite factory
     */
    private ModuleDescriptorFactory getChainedModuleDescriptorFactory(ModuleDescriptorFactory originalFactory)
    {
        // we really don't want two of these
        synchronized(this)
        {
            if (moduleDescriptorFactoryTracker == null)
                moduleDescriptorFactoryTracker = osgi.getServiceTracker(ModuleDescriptorFactory.class.getName());
        }

        // Really shouldn't be null, but could be in tests since we can't mock a service tracker :(
        if (moduleDescriptorFactoryTracker != null)
        {
            List<ModuleDescriptorFactory> factories = new ArrayList<ModuleDescriptorFactory>();
            Object[] serviceObjs = moduleDescriptorFactoryTracker.getServices();

            // Add all the dynamic module descriptor factories registered as osgi services
            if (serviceObjs != null)
            {
                for (Object fac : serviceObjs) factories.add((ModuleDescriptorFactory) fac);
            }

            // Put the application factory first
            factories.add(0, originalFactory);

            // Catch all unknown descriptors as unrecognised
            factories.add(new UnrecognisedModuleDescriptorFallbackFactory());

            return new ChainModuleDescriptorFactory(factories.toArray(new ModuleDescriptorFactory[]{}));
        }
        else
            return originalFactory;


    }

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }
}
