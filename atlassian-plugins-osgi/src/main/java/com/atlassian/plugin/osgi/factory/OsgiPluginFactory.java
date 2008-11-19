package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.descriptors.ChainModuleDescriptorFactory;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.DefaultPluginTransformer;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformer;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * Plugin loader that starts an OSGi container and loads plugins into it, wrapped as OSGi bundles.
 */
public class OsgiPluginFactory implements PluginFactory
{
    private static final Log log = LogFactory.getLog(OsgiPluginFactory.class);

    private final OsgiContainerManager osgi;
    private final PluginTransformer pluginTransformer;
    private final String pluginDescriptorFileName;
    private final DescriptorParserFactory descriptorParserFactory;

    private ServiceTracker moduleDescriptorFactoryTracker;

    public OsgiPluginFactory(String pluginDescriptorFileName, OsgiContainerManager osgi)
    {
        Validate.notNull(pluginDescriptorFileName, "Plugin descriptor is required");
        Validate.notNull(osgi, "The OSGi container is required");

        pluginTransformer = new DefaultPluginTransformer();
        this.osgi = osgi;
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new OsgiPluginXmlDescriptorParserFactory();
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");

        String pluginKey = null;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);

            if (descriptorStream != null)
            {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
                if (descriptorParser.getPluginsVersion() == 2)
                    pluginKey = descriptorParser.getKey();
            }
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

        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        PluginClassLoader loader = new PluginClassLoader(deploymentUnit.getPath());

        if (loader.getResource(pluginDescriptorFileName) == null)
            throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);

        try
        {
            ModuleDescriptorFactory combinedFactory = getChainedModuleDescriptorFactory(moduleDescriptorFactory);
            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);

            Bundle existingBundle = findBundle(parser.getKey(), parser.getPluginInformation().getVersion(), deploymentUnit);
            Plugin osgiPlugin;
            if (existingBundle != null)
            {
                osgiPlugin = new OsgiPlugin(existingBundle);
                log.info("OSGi bundle "+parser.getKey()+" found already installed.");
            }
            else
            {
                osgiPlugin = createOsgiPlugin(deploymentUnit.getPath());
            }
                
            plugin = parser.configurePlugin(combinedFactory, osgiPlugin);
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

            // Catch all unknown descriptors as deferred
            factories.add(new UnrecognisedModuleDescriptorFallbackFactory());

            return new ChainModuleDescriptorFactory(factories.toArray(new ModuleDescriptorFactory[]{}));
        }
        else
            return originalFactory;


    }

    private Plugin createOsgiPlugin(File file)
    {
        try
        {
            File transformedFile = pluginTransformer.transform(file, osgi.getHostComponentRegistrations());
            return new OsgiPlugin(osgi.installBundle(transformedFile));
        } catch (OsgiContainerException e)
        {
            return reportUnloadablePlugin(file, e);
        } catch (PluginTransformationException ex)
        {
            return reportUnloadablePlugin(file, ex);
        }
    }

    private Bundle findBundle(String key, String version, DeploymentUnit deploymentUnit)
    {
        for (Bundle bundle : osgi.getBundles())
        {
            if (key.equals(bundle.getSymbolicName()) && version.equals(bundle.getHeaders().get(Constants.BUNDLE_VERSION))
                    && deploymentUnit.lastModified() < bundle.getLastModified())
            {
                return bundle;
            }
        }
        return null;
    }

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }
}
