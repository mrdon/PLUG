package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
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

import java.io.File;
import java.io.InputStream;

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
        this.descriptorParserFactory = new ComponentFilteringXmlDescriptorParserFactory();
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");

        String pluginKey = null;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
        } catch (PluginParseException ex)
        {
            // no descriptor, no worries
        }

        if (descriptorStream != null)
        {
            final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
            if (descriptorParser.getPluginsVersion() == 2)
                pluginKey = descriptorParser.getKey();
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
            Plugin osgiPlugin = createOsgiPlugin(deploymentUnit.getPath());
            ModuleDescriptorFactory combinedFactory = getChainedModuleDescriptorFactory(osgiPlugin, moduleDescriptorFactory);
            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
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
    ModuleDescriptorFactory getChainedModuleDescriptorFactory(Plugin plugin, ModuleDescriptorFactory originalFactory)
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
            Object[] serviceObjs = moduleDescriptorFactoryTracker.getServices();
            ModuleDescriptorFactory[] dynamicFactories;
            if (serviceObjs != null && serviceObjs.length > 0)
            {
                dynamicFactories = new ModuleDescriptorFactory[serviceObjs.length + 2];
                System.arraycopy(serviceObjs, 0, dynamicFactories, 1, serviceObjs.length);
            }
            else
            {
                dynamicFactories = new ModuleDescriptorFactory[1];
            }
            dynamicFactories[0] = originalFactory;

            return new ChainModuleDescriptorFactory(dynamicFactories);
        }
        else
            return originalFactory;


    }

    Plugin createOsgiPlugin(File file)
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

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }
}
