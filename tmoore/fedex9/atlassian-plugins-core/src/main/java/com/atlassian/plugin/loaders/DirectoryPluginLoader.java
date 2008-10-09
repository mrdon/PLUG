package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.artifact.DefaultPluginArtifactFactory;
import com.atlassian.plugin.artifact.PluginArtifactFactory;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.Scanner;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

/**
 * A plugin loader to load plugins from a directory on disk.  A {@link Scanner} is used to locate plugin artifacts
 * and determine if they need to be redeployed or not.
 */
public class DirectoryPluginLoader implements DynamicPluginLoader
{
    private static Log log = LogFactory.getLog(DirectoryPluginLoader.class);
    private final Scanner scanner;
    private final Map<PluginArtifact,Plugin> plugins;
    private final List<PluginFactory> pluginFactories;
    private final PluginArtifactFactory pluginArtifactFactory;

    /**
     * Constructs a loader for a particular directory and set of deployers
     * @param path The directory containing the plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public DirectoryPluginLoader(File path, List<PluginFactory> pluginFactories,
                                 PluginEventManager pluginEventManager)
    {
        this(path, pluginFactories, new DefaultPluginArtifactFactory(), pluginEventManager);
    }

    /**
     * Constructs a loader for a particular directory and set of deployers
     * @param path The directory containing the plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginArtifactFactory The plugin artifact factory
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.1.0
     */
    public DirectoryPluginLoader(File path, List<PluginFactory> pluginFactories, PluginArtifactFactory pluginArtifactFactory,
                                 PluginEventManager pluginEventManager)
    {
        if (log.isDebugEnabled())
            log.debug("Creating plugin loader for url " + path);

        Validate.notNull(path, "The directory file must be specified");
        Validate.notNull(pluginFactories, "The list of plugin factories must be specified");
        Validate.notNull(pluginEventManager, "The event manager must be specified");

        scanner = new Scanner(path, pluginArtifactFactory);
        plugins = new HashMap<PluginArtifact,Plugin>();
        this.pluginFactories = new ArrayList<PluginFactory>(pluginFactories);
        this.pluginArtifactFactory = pluginArtifactFactory;
        pluginEventManager.register(this);
    }

    public Collection<Plugin> loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
    {
        scanner.scan();

        for (PluginArtifact pluginArtifact : scanner.getPluginArtifacts())
        {
            try
            {
                Plugin plugin = deployPluginArtifact(pluginArtifact, moduleDescriptorFactory);
                plugins.put(pluginArtifact, plugin);
            }
            catch (PluginParseException e)
            {
                // This catches errors so that the successfully loaded plugins can be returned.
                // It might be nicer if this method returned an object containing both the succesfully loaded
                // plugins and the unsuccessfully loaded plugins.
                log.error("Error loading descriptor for : " + pluginArtifact, e);
            }
        }

        if (scanner.getPluginArtifacts().size() == 0)
            log.info("No plugins found to be deployed");

        return plugins.values();
    }

    /**
     * @deprecated Since 2.1.0, use {@link #deployPluginArtifact(PluginArtifact, com.atlassian.plugin.ModuleDescriptorFactory)} instead
     */
    protected Plugin deployPluginFromUnit(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        return deployPluginArtifact(deploymentUnit, moduleDescriptorFactory);
    }

    /**
     * @since 2.1.0
     */
    protected Plugin deployPluginArtifact(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Plugin plugin = null;
        String errorText = "No plugin factories found for plugin file "+pluginArtifact;

        for (PluginFactory factory : pluginFactories)
        {
            try
            {
                if (factory.canCreate(pluginArtifact) != null)
                {
                    plugin = factory.create(pluginArtifact, moduleDescriptorFactory);
                    if (plugin != null)
                        break;
                }
            } catch (IllegalArgumentException ex)
            {
                errorText = ex.getMessage();
            }
        }
        if (plugin == null)
            plugin = new UnloadablePlugin(errorText);
        else
            log.info("Plugin " + pluginArtifact + " created");

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
    public Collection<Plugin> addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        // find missing plugins
        Collection<PluginArtifact> updatedDeploymentUnits = scanner.scanForArtifacts();

        // create list while updating internal state
        List<Plugin> foundPlugins = new ArrayList<Plugin>();
        for (PluginArtifact deploymentUnit : updatedDeploymentUnits)
        {
            if (!plugins.containsKey(deploymentUnit))
            {
                Plugin plugin = deployPluginArtifact(deploymentUnit, moduleDescriptorFactory);
                plugins.put(deploymentUnit, plugin);
                foundPlugins.add(plugin);
            }
        }
        if (foundPlugins.size() == 0)
            log.info("No plugins found to be installed");

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

        PluginArtifact deploymentUnit = findMatchingPluginArtifact(plugin);
        File pluginOnDisk = deploymentUnit.getFile();
        plugin.close();

        try
        {
            boolean found = false;
            for (PluginArtifact unit : plugins.keySet())
            {
                if(unit.getFile().equals(deploymentUnit.getFile()) && !unit.equals(deploymentUnit))
                {
                    found = true;
                    break;
                }
            }

            if (!found && !pluginOnDisk.delete())
                throw new PluginException("Could not delete plugin [" + plugin.getName() + "].");
        }
        catch (SecurityException e)
        {
            throw new PluginException(e);
        }

        scanner.clear(pluginOnDisk);
        plugins.remove(deploymentUnit);
        log.info("Removed plugin " + plugin.getKey());
    }

    private PluginArtifact findMatchingPluginArtifact(Plugin plugin)
            throws PluginException
    {
        PluginArtifact pluginArtifact = null;
        for (Iterator<Map.Entry<PluginArtifact,Plugin>> iterator = plugins.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry<PluginArtifact,Plugin> entry = iterator.next();
            // no, you don't want to use entry.getValue().equals(plugin) here as it breaks upgrades where it is a new
            // version of the plugin but the key and version number hasn't changed, and hence, equals() will always return
            // true
            if (entry.getValue() == plugin)
            {
                pluginArtifact = entry.getKey();
                break;
            }
        }

        if (pluginArtifact == null) //the pluginLoader has no memory of deploying this plugin
            throw new PluginException("This pluginLoader has no memory of deploying the plugin you are trying remove: [" + plugin.getName() + "]" );
        return pluginArtifact;
    }

    /**
     * Called during plugin framework shutdown
     * @param event The shutdown event
     */
    @PluginEventListener
    public void onShutdown(PluginFrameworkShutdownEvent event)
    {
        scanner.clearAll();
        for (Iterator<Plugin> it = plugins.values().iterator(); it.hasNext();)
        {
            Plugin plugin  = it.next();
            plugin.close();
            it.remove();
        }
    }

    /**
     * @deprecated Since 2.0.0, shutdown will automatically occur when the plugin framework is shutdown
     */
    public void shutDown()
    {
        onShutdown(null);
    }

    /**
     * Determines if the artifact can be loaded by any of its deployers
     *
     * @param pluginArtifact The artifact to test
     * @return True if this artifact can be loaded by this loader
     * @throws PluginParseException
     */
    public String canLoad(PluginArtifact pluginArtifact) throws PluginParseException
    {
        String pluginKey = null;
        for (PluginFactory factory : pluginFactories)
        {
            pluginKey = factory.canCreate(pluginArtifact);
            if (pluginKey != null)
                break;
        }
        return pluginKey;
    }
}
