package com.atlassian.plugin;

import com.atlassian.plugin.loaders.PluginLoader;

import java.util.*;

public class DefaultPluginManager implements PluginManager
{
    private final List pluginLoaders;
    private final PluginStateStore store;
    private Map moduleDescriptors;
    private PluginManagerState currentState;
    private HashMap plugins;

    public DefaultPluginManager(PluginStateStore store, List pluginLoaders, Map moduleDescriptors)
    {
        this.pluginLoaders = pluginLoaders;
        this.store = store;
        this.currentState = store.loadPluginState();
        this.moduleDescriptors = moduleDescriptors;
        this.plugins = new HashMap();
    }

    public void init()
    {
        // retrieve all the plugins
        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();

            for (Iterator iterator1 = loader.getPlugins(moduleDescriptors).iterator(); iterator1.hasNext();)
            {
                addPlugin((Plugin) iterator1.next());
            }
        }
    }

    private void addPlugin(Plugin plugin)
    {
        plugins.put(plugin.getKey(), plugin);
    }

    private void saveState()
    {
        store.savePluginState(currentState);
    }

    public Collection getPlugins()
    {
        return plugins.values();
    }

    public Plugin getPlugin(String key)
    {
        return (Plugin) plugins.get(key);
    }


    public void enableLibrary(String key)
    {
        if (key == null)
            throw new IllegalArgumentException("You must specify a plugin key to disable.");

        if (plugins.containsKey(key))
        {
            Plugin plugin = (Plugin) plugins.get(key);

            if (!plugin.isEnabledByDefault())
                currentState.setPluginState(key, Boolean.TRUE);
            else
                currentState.removePluginState(key);

            saveState();
        }
    }

    public void disablePlugin(String key)
    {
        if (key == null)
            throw new IllegalArgumentException("You must specify a plugin key to disable.");

        if (plugins.containsKey(key))
        {
            Plugin plugin = (Plugin) plugins.get(key);
            if (plugin.isEnabledByDefault())
                currentState.setPluginState(key, Boolean.FALSE);
            else
                currentState.removePluginState(key);

            saveState();
        }
    }

    public boolean isPluginEnabled(String key)
    {
        return plugins.containsKey(key) && currentState.isEnabled((Plugin) plugins.get(key));
    }

}