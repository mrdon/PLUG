package com.atlassian.plugin;

import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.impl.DynamicPlugin;

import java.util.*;
import java.io.InputStream;

public class DefaultPluginManager implements PluginManager
{
    private final List pluginLoaders;
    private final PluginStateStore store;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private PluginManagerState currentState;
    private HashMap plugins;
    private HashMap licensedPlugins;
    private HashMap dynamicPlugins;
    private HashMap pluginLoaderToPlugin; //will store a pluginLoader as a key and the entry will be a list of plugins


    public DefaultPluginManager(PluginStateStore store, List pluginLoaders, ModuleDescriptorFactory moduleDescriptorFactory)
    {
        this.pluginLoaders = pluginLoaders;
        this.store = store;
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        this.currentState = store.loadPluginState();
    }




    /**
     * Initialize all plugins the first time.
     * @throws PluginParseException
     */
    public void init() throws PluginParseException
    {
        this.plugins = new HashMap();
        this.licensedPlugins = new HashMap();
        this.dynamicPlugins = new HashMap();

        // retrieve all the plugins
        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();

            for (Iterator iterator1 = loader.loadAllPlugins(moduleDescriptorFactory).iterator(); iterator1.hasNext();)
            {
                addPlugin((Plugin) iterator1.next());
//                addPlugin(pluginLoader, plugin); //stores the plugin with pluginloader
            }
        }
    }

    public void destroy(Plugin plugin)
    {
        //locate the individual plugin from pluginLoaderToPlugin
        //call destroy
    }

    /**
     * Re-initialize all plugins - this will destroy every plugin and then initialize afresh.
     * @param plugin
     * @throws PluginParseException
     */
    public void reInit() throws PluginParseException
    {
        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();

            if (loader.supportsRemoval())
            {
                Collection removedPlugins = loader.removeMissingPlugins();
                // remove each one
            }

            if (loader.supportsAddition())
            {
                Collection addedPlugins = loader.addFoundPlugins(moduleDescriptorFactory);
                // add each one
            }
        }
    }

    /**
     * Shutdown the plugin system by disabling (optionally) and then destroying all modules.
     * <p>
     * Be very careful when using this method - plugin system will not be in a usable state afterwards.
     * <p>
     * @see DefaultPluginManager#reInit();
     */
    public void destroy()
    {
        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            // remove the plugin from the plugins map
            Plugin plugin = (Plugin) iterator.next();
            iterator.remove();

            for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) it.next();

                // if the module is StateAware, then disable it (matches enable())
                if (descriptor instanceof StateAware && isPluginModuleEnabled(descriptor.getCompleteKey()))
                    ((StateAware)descriptor).disabled();

                // now destroy it (matches init())
                descriptor.destroy(plugin);
            }
        }
    }

    protected void addPlugin(Plugin plugin) throws PluginParseException
    {
        // testing to make sure plugin keys are unique
        if (plugins.containsKey(plugin.getKey()) || licensedPlugins.containsKey(plugin.getKey()))
            throw new PluginParseException("Duplicate plugin key found: '" + plugin.getKey() + "'");

        plugins.put(plugin.getKey(), plugin);

        // Store plugins requiring a license
        if (plugin.getPluginInformation() != null
                && plugin.getPluginInformation().getLicenseRegistryLocation() != null 
                && plugin.getPluginInformation().getLicenseRegistryLocation() != "")
            licensedPlugins.put(plugin.getName(), plugin);

        if (plugin instanceof DynamicPlugin)
            dynamicPlugins.put(plugin.getName(), plugin);

        for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) it.next();
            if (descriptor instanceof StateAware && isPluginModuleEnabled(descriptor.getCompleteKey()))
                ((StateAware)descriptor).enabled();
        }
    }

    private void saveState()
    {
        store.savePluginState(currentState);
    }

    public Collection getPlugins()
    {
        return plugins.values();
    }

    public Collection getEnabledPlugins()
    {
        List result = new ArrayList();

        for (Iterator iterator = plugins.keySet().iterator(); iterator.hasNext();)
        {
            String key = (String) iterator.next();
            Plugin p = getEnabledPlugin(key);

            if (p != null)
                result.add(p);
        }

        return result;
    }

    public Plugin getPlugin(String key)
    {
        return (Plugin) plugins.get(key);
    }

    public Plugin getEnabledPlugin(String pluginKey)
    {
        if (isPluginEnabled(pluginKey))
            return getPlugin(pluginKey);

        return null;
    }

    public ModuleDescriptor getPluginModule(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        final Plugin plugin = getPlugin(key.getPluginKey());

        if (plugin != null)
            return plugin.getModuleDescriptor(key.getModuleKey());

        return null;
    }

    public ModuleDescriptor getEnabledPluginModule(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        if (isPluginModuleEnabled(completeKey))
        {
            return getEnabledPlugin(key.getPluginKey()).getModuleDescriptor(key.getModuleKey());
        }

        return null;
    }

    public List getEnabledModulesByClass(Class moduleClass)
    {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();

            if (isPluginEnabled(plugin.getKey()))
            {
                for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
                {
                    ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator1.next();

                    if (!isPluginModuleEnabled(moduleDescriptor.getCompleteKey()))
                        continue;

                    final Class moduleDescClass = moduleDescriptor.getModuleClass();
                    if (moduleDescClass != null && moduleClass.isAssignableFrom(moduleDescClass))
                    {
                        final Object module = moduleDescriptor.getModule();
                        if (module != null)
                        {
                            result.add(module);
                        }
                    }
                }
            }
        }

        return result;
    }


    public void enablePlugin(String key)
    {
        if (key == null)
            throw new IllegalArgumentException("You must specify a plugin key to disable.");

        if (plugins.containsKey(key))
        {
            Plugin plugin = (Plugin) plugins.get(key);

            if (!plugin.isEnabledByDefault())
                currentState.setState(key, Boolean.TRUE);
            else
                currentState.removeState(key);

            for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) it.next();
                if (descriptor instanceof StateAware && isPluginModuleEnabled(descriptor.getCompleteKey()))
                    ((StateAware)descriptor).enabled();
            }
            
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

            for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) it.next();
                if (descriptor instanceof StateAware && isPluginModuleEnabled(descriptor.getCompleteKey()))
                    ((StateAware)descriptor).disabled();
            }

            if (plugin.isEnabledByDefault())
                currentState.setState(key, Boolean.FALSE);
            else
                currentState.removeState(key);


            saveState();
        }
    }

    public void disablePluginModule(String completeKey)
    {
        if (completeKey == null)
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");

        final ModuleDescriptor module = getPluginModule(completeKey);
        if (module != null)
        {
            if (module.isEnabledByDefault())
                currentState.setState(completeKey, Boolean.FALSE);
            else
                currentState.removeState(completeKey);

            if (module instanceof StateAware)
                ((StateAware) module).disabled();

            saveState();
        }
    }

    public void enablePluginModule(String completeKey)
    {
        if (completeKey == null)
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");

        final ModuleDescriptor module = getPluginModule(completeKey);
        if (module != null)
        {
            if (!module.isEnabledByDefault())
                currentState.setState(completeKey, Boolean.TRUE);
            else
                currentState.removeState(completeKey);

            if (module instanceof StateAware)
                ((StateAware) module).enabled();

            saveState();
        }
    }

    public boolean isPluginModuleEnabled(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        final ModuleDescriptor pluginModule = getPluginModule(completeKey);
        return isPluginEnabled(key.getPluginKey()) && pluginModule != null && currentState.isEnabled(pluginModule);
    }

    public boolean isPluginEnabled(String key)
    {
        return plugins.containsKey(key) && currentState.isEnabled((Plugin) plugins.get(key));
    }

    public List getEnabledModuleDescriptorsByClass(Class descriptorClazz)
    {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();

            if (isPluginEnabled(plugin.getKey()))
            {
                for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
                {
                    ModuleDescriptor module = (ModuleDescriptor) iterator1.next();

                    if (descriptorClazz.isInstance(module) && isPluginModuleEnabled(module.getCompleteKey()))
                    {
                        result.add(module);
                    }
                }
            }
        }

        return result;
    }

    /**
     * @throws IllegalArgumentException If the name is not a registered module descriptor
     */
    public List getEnabledModuleDescriptorsByType(String type) throws PluginParseException, IllegalArgumentException
    {
        final Class descriptorClazz = (Class) moduleDescriptorFactory.getModuleDescriptorClass(type);

        if (descriptorClazz == null)
            throw new IllegalArgumentException("No module descriptor of type: " + type + " found.");

        return getEnabledModuleDescriptorsByClass(descriptorClazz);
    }

    public HashMap getLicensedPluginsMap()
    {
        return licensedPlugins;
    }

    public InputStream getDynamicResourceAsStream(String name)
    {
        Iterator it = dynamicPlugins.values().iterator();

        while (it.hasNext())
        {
            DynamicPlugin dynamicPlugin = (DynamicPlugin) it.next();
            if (isPluginEnabled(dynamicPlugin.getKey()))
            {
                InputStream is = dynamicPlugin.getResourceAsStream(name);

                if (is != null)
                    return is;
            }
        }

        return null;
    }
}