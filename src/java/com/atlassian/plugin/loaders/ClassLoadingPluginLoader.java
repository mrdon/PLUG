package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import com.atlassian.plugin.util.FileUtils;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.PluginsClassLoader;
import com.atlassian.plugin.loaders.classloading.Scanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * A plugin loader to load plugins from a classloading on disk.
 * <p>
 * This creates a classloader for that classloading and loads plugins from all JARs from within it.
 */
public class ClassLoadingPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassLoadingPluginLoader.class);
    private String fileNameToLoad;
    private Scanner scanner;
    private Map plugins;
    private DescriptorParserFactory descriptorParserFactory = new XmlDescriptorParserFactory();

    public ClassLoadingPluginLoader(File path)
    {
        this(path, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
    }

    public ClassLoadingPluginLoader(File path, String fileNameToLoad)
    {
        log.debug("Creating classloader for url " + path);
        scanner = new Scanner(path);
        this.fileNameToLoad = fileNameToLoad;
        plugins = new HashMap();
    }

    public Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        scanner.scan();

        for (Iterator iterator = scanner.getDeploymentUnits().iterator(); iterator.hasNext();)
        {
            DeploymentUnit deploymentUnit = (DeploymentUnit) iterator.next();
            try
            {
                Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
                plugins.put(deploymentUnit, plugin);
            }
            catch (PluginParseException e)
            {
                // The exception has already been logged. We just ignore it and move on to the next
                // plugin
            }
        }

        return plugins.values();
    }

    /**
     * @return the plugin loaded from the deployment unit, or an UnloadablePlugin instance if loading fails.
     */
    protected Plugin deployPluginFromUnit(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Plugin plugin = null;

        PluginsClassLoader loader = getPluginsClassLoader(deploymentUnit);

        URL pluginDescriptor = loader.getResource(fileNameToLoad);

        if (pluginDescriptor == null)
        {
            log.error("No descriptor found in classloader for : " + deploymentUnit);
        }
        else
        {
            InputStream is = loader.getResourceAsStream(fileNameToLoad);
            try
            {
                // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
                DescriptorParser parser = descriptorParserFactory.getInstance(is);
                plugin = parser.configurePlugin(moduleDescriptorFactory, new DynamicPlugin(deploymentUnit, loader));
            }
            catch (PluginParseException e)
            {
                log.error("Error loading descriptor for : " + deploymentUnit, e);

                // Under normal conditions, the loader would be closed when the plugins are undeployed. However,
                // these are not normal conditions, so we need to make sure that we close them explicitly.
                loader.close();
                throw e;
            }
            finally
            {
                FileUtils.shutdownStream(is);
            }
        }
        return plugin;
    }

    private PluginsClassLoader getPluginsClassLoader(DeploymentUnit deploymentUnit)
    {
        return (PluginsClassLoader) scanner.getClassLoader(deploymentUnit);
    }

    public boolean supportsRemoval()
    {
        return true;
    }

    public boolean supportsAddition()
    {
        return true;
    }

    /**
     * @return all plugins, now loaded by the pluginLoader, which have been discovered and added since the
     * last time a check was performed.
     */
    public Collection addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        // find missing plugins
        scanner.scan();

        // create list while updating internal state
        List foundPlugins = new ArrayList();
        for (Iterator iterator = scanner.getDeploymentUnits().iterator(); iterator.hasNext();)
        {
            DeploymentUnit deploymentUnit = (DeploymentUnit) iterator.next();
            if (!plugins.containsKey(deploymentUnit))
            {
                Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
                plugins.put(deploymentUnit, plugin);
                foundPlugins.add(plugin);
                // iterator.remove();
            }
        }

        return foundPlugins;
    }

    /**
     * @param plugin - the plugin to remove
     * @throws PluginException representing the reason for failure.
     */
    public void removePlugin(Plugin plugin) throws PluginException
    {
        if (plugin.isEnabled())
            throw new PluginException("Cannot remove an enabled plugin");

        if (!plugin.isUninstallable())
        {
            throw new PluginException("Cannot remove an uninstallable plugin: [" + plugin.getName() + "]" );
        }

        //find the matching deployment unit
        Iterator iterator = plugins.keySet().iterator();
        DeploymentUnit deploymentUnit = null;

        while (iterator.hasNext())
        {
            Object o = iterator.next();

            if (plugins.get(o) == plugin)
            {
                deploymentUnit = (DeploymentUnit) o;
                plugins.remove(deploymentUnit);
                break;
            }
        }

        if (deploymentUnit == null) //the pluginLoader has no memory of deploying this plugin
            throw new PluginException("This pluginLoader has no memory of deploying the plugin you are trying remove: [" + plugin.getName() + "]" );

        //delete the plugin from the filesystem
        File pluginOnDisk = deploymentUnit.getPath();
        scanner.undeploy(pluginOnDisk);

        try
        {
            if (!pluginOnDisk.delete())
                throw new PluginException("Could not delete plugin [" + plugin.getName() + "].");
        }
        catch (SecurityException e)
        {
            throw new PluginException(e);
        }
    }

    public void shutDown()
    {
        scanner.undeployAll();
    }
}
