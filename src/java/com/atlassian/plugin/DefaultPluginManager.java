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

    public void init() throws PluginParseException {
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

    private void addPlugin(Plugin plugin) throws PluginParseException
    {
        // testing to make sure plugin keys are unique
        if (plugins.containsKey(plugin.getKey()))
            throw new PluginParseException("Duplicate plugin key found: '" + plugin.getKey() + "'");

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

    public ModuleDescriptor getPluginModule(String completeKey)
    {
        final int sepIdx = completeKey.indexOf(":");

        if (sepIdx <= 0)
            throw new IllegalArgumentException("Invalid complete key specified: " + completeKey);

        String pluginKey = completeKey.substring(0, sepIdx);
        String moduleKey = completeKey.substring(sepIdx + 1);

        return getPlugin(pluginKey).getModule(moduleKey);
    }

    public Collection getEnabledModulesByClass(Class moduleClass)
    {
        for (Iterator iterator = plugins.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            Plugin plugin = (Plugin) entry.getValue();
            List result = plugin.getModulesByClass(moduleClass);

            if (!result.isEmpty())
                return result;
        }

        return Collections.EMPTY_LIST;
    }


    public void enablePlugin(String key)
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

    public List getEnabledModuleDescriptorsByClass(Class descriptorClazz) {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();) {
            Plugin plugin = (Plugin) iterator.next();

            if (isPluginEnabled(plugin.getKey()))
            {
                for (Iterator iterator1 = plugin.getModules().iterator(); iterator1.hasNext();)
                {
                    ModuleDescriptor module = (ModuleDescriptor) iterator1.next();

                    if (descriptorClazz.isInstance(module))
                    {
                        result.add(module);
                    }
                }
            }
        }

        return result;
    }

}