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
import com.atlassian.plugin.loaders.classloading.JarClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * A plugin loader to load plugins from a classloading on disk.
 * <p>
 * This creates a classloader for that classloading and loads plugins from all JARs from within it.
 */
public class ClassLoadingPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassLoadingPluginLoader.class);
    private final String pluginDescriptorFileName;
    private final Scanner scanner;
    /** Maps {@link DeploymentUnit}s to {@link Plugin}s. */
    private final Map plugins;
    private final DescriptorParserFactory descriptorParserFactory;

    public ClassLoadingPluginLoader(File path)
    {
        this(path, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
    }

    public ClassLoadingPluginLoader(File path, String pluginDescriptorFileName)
    {
        log.debug("Creating classloader for url " + path);
        scanner = new Scanner(path);
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
        plugins = new HashMap();
    }

    public Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
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
                // This catches errors so that the successfully loaded plugins can be returned.
                // It might be nicer if this method returned an object containing both the succesfully loaded
                // plugins and the unsuccessfully loaded plugins.
                log.error("Error loading descriptor for : " + deploymentUnit, e);
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
        InputStream pluginDescriptor = null;
        PluginsClassLoader loader = new JarClassLoader(deploymentUnit.getPath(), Thread.currentThread().getContextClassLoader());
        try
        {
            if (loader.getResource(pluginDescriptorFileName) == null)
                throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);

            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
            plugin = parser.configurePlugin(moduleDescriptorFactory, new DynamicPlugin(deploymentUnit, loader));
        }
        // Under normal conditions, the loader would be closed when the plugins are undeployed. However,
        // these are not normal conditions, so we need to make sure that we close them explicitly.        
        catch (PluginParseException e)
        {
            loader.close();
            throw e;
        }
        catch (RuntimeException e)
        {
            loader.close();
            throw e;
        }
        catch (Error e)
        {
            loader.close();
            throw e;
        }
        finally
        {
            FileUtils.shutdownStream(pluginDescriptor);
        }
        return plugin;
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
        Collection updatedDeploymentUnits = scanner.scan();

        // create list while updating internal state
        List foundPlugins = new ArrayList();
        for (Iterator it = updatedDeploymentUnits.iterator(); it.hasNext();)
        {
            DeploymentUnit deploymentUnit = (DeploymentUnit) it.next();
            if (!plugins.containsKey(deploymentUnit))
            {
                Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
                plugins.put(deploymentUnit, plugin);
                foundPlugins.add(plugin);
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

        DeploymentUnit deploymentUnit = findMatchingDeploymentUnit(plugin);
        File pluginOnDisk = deploymentUnit.getPath();

        try
        {
            plugin.close();

            if (!pluginOnDisk.delete())
                throw new PluginException("Could not delete plugin [" + plugin.getName() + "].");
        }
        catch (SecurityException e)
        {
            throw new PluginException(e);
        }

        scanner.clear(pluginOnDisk);
        plugins.remove(deploymentUnit);
    }

    private DeploymentUnit findMatchingDeploymentUnit(Plugin plugin)
            throws PluginException
    {
        DeploymentUnit deploymentUnit = null;
        for (Iterator iterator = plugins.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (entry.getValue() == plugin)
                deploymentUnit = (DeploymentUnit) entry.getKey();
        }

        if (deploymentUnit == null) //the pluginLoader has no memory of deploying this plugin
            throw new PluginException("This pluginLoader has no memory of deploying the plugin you are trying remove: [" + plugin.getName() + "]" );
        return deploymentUnit;
    }

    public void shutDown()
    {
        scanner.clearAll();
        for (Iterator it = plugins.values().iterator(); it.hasNext();)
        {
            Plugin plugin  = (Plugin) it.next();
            plugin.close();
        }
    }
}
