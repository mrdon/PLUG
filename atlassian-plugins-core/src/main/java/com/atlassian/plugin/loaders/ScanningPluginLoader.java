package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.classloading.*;
import com.atlassian.plugin.loaders.classloading.Scanner;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

/**
 * Plugin loader that delegates the detection of plugins to a Scanner instance. The scanner may monitor the contents
 * of a directory on disk, a database, or any other place plugins may be hidden.
 *
 * @since 2.1.0
 */
public class ScanningPluginLoader implements DynamicPluginLoader
{
    private static Log log = LogFactory.getLog(DirectoryPluginLoader.class);
    protected final com.atlassian.plugin.loaders.classloading.Scanner scanner;
    protected final Map<DeploymentUnit,Plugin> plugins;
    protected final List<PluginFactory> pluginFactories;
    protected final PluginArtifactFactory pluginArtifactFactory;

    /**
     * Constructor that provides a default plugin artifact factory
     *
     * @param scanner The scanner to use to detect new plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public ScanningPluginLoader(Scanner scanner, List<PluginFactory> pluginFactories,
                                 PluginEventManager pluginEventManager)
    {
        this(scanner, pluginFactories, new DefaultPluginArtifactFactory(), pluginEventManager);
    }

    /**
     * Construct a new scanning plugin loader with no default values
     *
     * @param scanner The scanner to use to detect new plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginArtifactFactory used to create new plugin artifacts from an URL
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public ScanningPluginLoader(Scanner scanner, List<PluginFactory> pluginFactories, PluginArtifactFactory pluginArtifactFactory,
                                 PluginEventManager pluginEventManager)
    {
        Validate.notNull(pluginFactories, "The list of plugin factories must be specified");
        Validate.notNull(pluginEventManager, "The event manager must be specified");
        Validate.notNull(scanner, "The scanner must be specified");

        this.plugins = new HashMap<DeploymentUnit,Plugin>();

        this.pluginArtifactFactory = pluginArtifactFactory;
        this.scanner = scanner;
        this.pluginFactories = new ArrayList<PluginFactory>(pluginFactories);

        pluginEventManager.register(this);
    }

    public Collection<Plugin> loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
    {
        scanner.scan();

        for (DeploymentUnit deploymentUnit : scanner.getDeploymentUnits())
        {
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

        if (scanner.getDeploymentUnits().isEmpty())
            log.info("No plugins found to be deployed");

        return plugins.values();
    }

    protected Plugin deployPluginFromUnit(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Plugin plugin = null;
        String errorText = "No plugin factories found for plugin file "+deploymentUnit;

        for (PluginFactory factory : pluginFactories)
        {
            try
            {
                PluginArtifact artifact = pluginArtifactFactory.create(deploymentUnit.getPath().toURI());
                if (factory.canCreate(artifact) != null)
                {
                    plugin = factory.create(deploymentUnit, moduleDescriptorFactory);
                    if (plugin != null)
                        break;
                }
            }
            catch (IllegalArgumentException ex)
            {
                errorText = ex.getMessage();
            }
        }
        if (plugin == null)
            plugin = new UnloadablePlugin(errorText);
        else
            log.info("Plugin " + deploymentUnit + " created");

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
        Collection<DeploymentUnit> updatedDeploymentUnits = scanner.scan();

        // create list while updating internal state
        List<Plugin> foundPlugins = new ArrayList<Plugin>();
        for (DeploymentUnit deploymentUnit : updatedDeploymentUnits)
        {
            if (!plugins.containsKey(deploymentUnit))
            {
                Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
                plugins.put(deploymentUnit, plugin);
                foundPlugins.add(plugin);
            }
        }
        if (foundPlugins.isEmpty())
            log.info("No plugins found to be installed");

        return foundPlugins;
    }

    /**
     * @param plugin - the plugin to remove
     * @throws com.atlassian.plugin.PluginException representing the reason for failure.
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
        plugin.close();

        try
        {
            // Loop over to see if there are any other deployment units with the same filename. This will happen
            // if a newer plugin is uploaded with the same filename as the plugin being removed: in this case the
            // old one has already been deleted
            boolean found = false;
            for (DeploymentUnit unit : plugins.keySet())
            {
                if(unit.getPath().equals(deploymentUnit.getPath()) && !unit.equals(deploymentUnit))
                {
                    found = true;
                    break;
                }
            }

            if (!found)
                scanner.remove(deploymentUnit);
        }
        catch (SecurityException e)
        {
            throw new PluginException(e);
        }

        plugins.remove(deploymentUnit);
        log.info("Removed plugin " + plugin.getKey());
    }

    private DeploymentUnit findMatchingDeploymentUnit(Plugin plugin)
            throws PluginException
    {
        DeploymentUnit deploymentUnit = null;
        for (Map.Entry<DeploymentUnit, Plugin> entry : plugins.entrySet())
        {
            // no, you don't want to use entry.getValue().equals(plugin) here as it breaks upgrades where it is a new
            // version of the plugin but the key and version number hasn't changed, and hence, equals() will always return
            // true
            if (entry.getValue() == plugin)
            {
                deploymentUnit = entry.getKey();
                break;
            }
        }

        if (deploymentUnit == null) //the pluginLoader has no memory of deploying this plugin
            throw new PluginException("This pluginLoader has no memory of deploying the plugin you are trying remove: [" + plugin.getName() + "]" );
        return deploymentUnit;
    }

    /**
     * Called during plugin framework shutdown
     * @param event The shutdown event
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @PluginEventListener
    public void onShutdown(PluginFrameworkShutdownEvent event)
    {
        for (Iterator<Plugin> it = plugins.values().iterator(); it.hasNext();)
        {
            Plugin plugin  = it.next();
            plugin.close();
            it.remove();
        }

        scanner.reset();
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
     * @throws com.atlassian.plugin.PluginParseException
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
