package com.atlassian.plugin;

import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptorFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.impl.UnloadablePluginFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.*;

public class DefaultPluginManager implements PluginManager
{
    private static final Log log = LogFactory.getLog(DefaultPluginManager.class);
    private final List pluginLoaders;
    private final PluginStateStore store;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    //private PluginManagerState currentState;
    private HashMap plugins = new HashMap();
    private HashMap pluginToPluginLoader = new HashMap(); // will store a plugin as a key and pluginLoader as a value

    public DefaultPluginManager(PluginStateStore store, List pluginLoaders, ModuleDescriptorFactory moduleDescriptorFactory)
    {
        this.pluginLoaders = pluginLoaders;
        this.store = store;
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        //this.currentState = store.loadPluginState();
    }

    /**
     * Initialize all plugins the first time.
     *
     * @throws PluginParseException
     */
    public void init() throws PluginParseException
    {
        // retrieve all the plugins
        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();

            if(loader != null){
                for (Iterator iterator1 = loader.loadAllPlugins(moduleDescriptorFactory).iterator(); iterator1.hasNext();)
                {
                    addPlugin(loader, (Plugin) iterator1.next());
                }
            }
        }
    }

    public int scanForNewPlugins() throws PluginParseException
    {
        int numberFound = 0;

        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();

            if(loader != null){
                if (loader.supportsAddition())
                {
                    Collection addedPlugins = loader.addFoundPlugins(moduleDescriptorFactory);
                    for (Iterator iterator1 = addedPlugins.iterator(); iterator1.hasNext();)
                    {
                        Plugin newPlugin = (Plugin) iterator1.next();
                        numberFound++;
                        addPlugin(loader, newPlugin);
                    }
                }
            }
        }

        return numberFound;
    }

    /**
     * Uninstall (delete) a plugin if possible.
     * <p/>
     * Be very careful when using this method, the plugin cannot be brought back.
     */
    public void uninstall(Plugin plugin) throws PluginException
    {
        if (!plugin.isUninstallable())
            throw new PluginException("Plugin is not uninstallable: " + plugin.getKey());

        if (isPluginEnabled(plugin.getKey()))
            disablePlugin(plugin.getKey());

        PluginLoader loader = (PluginLoader) pluginToPluginLoader.remove(plugin);

        if (loader == null || !loader.supportsRemoval())
        {
            throw new PluginException("Not uninstalling plugin - could not find plugin loader, or loader doesn't allow removal. Plugin: " + plugin.getKey());
        }

        plugins.remove(plugin.getKey());

        for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) it.next();

            // if the module is StateAware, then disable it (matches enable())
            disableIfStateAwareAndEnabled(descriptor);

            // now destroy it (matches init())
            descriptor.destroy(plugin);
        }

        // PLUG-13. Plugins should not save state across uninstalls.
        PluginManagerState currentState = getState();
        currentState.removeState(plugin.getKey());
        saveState(currentState);

        if(plugin.isDeleteable())
          loader.removePlugin(plugin);
    }

    private void disableIfStateAwareAndEnabled(ModuleDescriptor descriptor)
    {
        if (descriptor instanceof StateAware)
            {
                StateAware stateAware = (StateAware)descriptor;
                if (isPluginModuleEnabled(descriptor.getCompleteKey()))
                {
                    stateAware.disabled();
                }
            }
    }

    private PluginManagerState getState()
    {
        return store.loadPluginState();
    }


    protected void addPlugin(PluginLoader loader, Plugin plugin) throws PluginParseException
    {
        // testing to make sure plugin keys are unique
        if (plugins.containsKey(plugin.getKey())) {
          Plugin match = (Plugin) plugins.get(plugin.getKey());
          if(plugin.compareTo(match) > 0 ){
              if (log.isDebugEnabled())
                  log.debug("We found a newer '" + plugin.getKey() + "'");
              try
              {
                  log.info("Uninstalling " + match.getName() + " to upgrade.");
                  uninstall(match);
                  if (log.isDebugEnabled())
                      log.debug("Older '" + plugin.getKey() + "' uninstalled.");
              } catch (PluginException e) {
                  throw new PluginParseException("Duplicate plugin found (installed version is older) and could not be removed: '" + plugin.getKey() + "'");
              }
          } else {
              // If we find an older plugin, don't error, just ignore it. PLUG-12.
              if (log.isDebugEnabled())
                  log.debug("Duplicate plugin found (installed version is the same or newer): '" + plugin.getKey() + "'");
              // and don't install the older plugin
              return;
          }
        }

        plugins.put(plugin.getKey(), plugin);

        List moduleDescriptors = new ArrayList(plugin.getModuleDescriptors());

        for (Iterator it = moduleDescriptors.iterator(); it.hasNext();)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) it.next();

            if (!(descriptor instanceof StateAware))
            {
                if (log.isDebugEnabled())
                    log.debug("ModuleDescriptor '" + descriptor.getName() + "' is not StateAware. No need to enable.");

                continue;
            }

            if (!isPluginModuleEnabled(descriptor.getCompleteKey()))
            {
                if (log.isDebugEnabled())
                    log.debug("Plugin is not enabled, so not enabling ModuleDescriptor '" + descriptor.getName() + "'.");

                continue;
            }

            try
            {
                ((StateAware) descriptor).enabled();
            }
            /**
             * Catch any exceptions thrown during the enabling of the plugin (PLUG-7)
             *
             * When a problem occurs, we should catch the throwable and make an UnloadablePlugin.
             */
            catch (Throwable t)
            {
                log.error("There was an error loading the descriptor '" + descriptor.getName() + "' of plugin '" + plugin.getKey() + "'. Disabling.", t);

                UnloadableModuleDescriptor unloadableDescriptor = UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin, descriptor, t);

                UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin, unloadableDescriptor);

                // Replace the plugin
                replacePluginWithUnloadablePlugin(plugin, unloadablePlugin);

                plugin = unloadablePlugin;
            }
        }

        pluginToPluginLoader.put(plugin, loader);
    }

    private void saveState(PluginManagerState state)
    {
        store.savePluginState(state);
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
        if (!isPluginEnabled(pluginKey))
            return null;

        return getPlugin(pluginKey);
    }

    public ModuleDescriptor getPluginModule(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        final Plugin plugin = getPlugin(key.getPluginKey());

        if (plugin == null)
            return null;

        return plugin.getModuleDescriptor(key.getModuleKey());
    }

    public ModuleDescriptor getEnabledPluginModule(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        // If it's disabled, return null
        if (!isPluginModuleEnabled(completeKey))
            return null;

        return getEnabledPlugin(key.getPluginKey()).getModuleDescriptor(key.getModuleKey());
    }

    public List getEnabledModulesByClass(Class moduleClass)
    {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();

            // Skip disabled plugins
            if (!isPluginEnabled(plugin.getKey()))
                continue;

            for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
            {
                ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator1.next();

                if (!isPluginModuleEnabled(moduleDescriptor.getCompleteKey()))
                    continue;

                final Class moduleDescClass = moduleDescriptor.getModuleClass();
                if (moduleDescClass != null && moduleClass.isAssignableFrom(moduleDescClass))
                {
                    try
                    {
                        final Object module = moduleDescriptor.getModule();
                        if (module != null)
                        {
                            result.add(module);
                        }
                    }
                    catch (Exception e)
                    {
                        log.error(e);
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

        if (!plugins.containsKey(key))
        {
            if (log.isInfoEnabled())
                log.info("No plugin was found for key '" + key + "'. Not enabling.");

            return;
        }

        Plugin plugin = (Plugin) plugins.get(key);

        if (!plugin.getPluginInformation().satisfiesMinJavaVersion())
        {
            log.error("Minimum Java version of '" + plugin.getPluginInformation().getMinJavaVersion() + "' was not satisfied for module '" + key + "'. Not enabling.");
            return;
        }
        PluginManagerState currentState = getState();
        if (!plugin.isEnabledByDefault())
            currentState.setState(key, Boolean.TRUE);
        else
            currentState.removeState(key);
        saveState(currentState);

        notifyPluginEnabled(plugin);
    }

    protected void notifyPluginEnabled(Plugin plugin)
    {
        List moduleDescriptors = new ArrayList(plugin.getModuleDescriptors());

        for (Iterator it = moduleDescriptors.iterator(); it.hasNext();)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) it.next();

            if (!(descriptor instanceof StateAware))
            {
                if (log.isDebugEnabled())
                    log.debug("ModuleDescriptor '" + descriptor.getName() + "' is not StateAware. No need to enable.");

                continue;
            }

            if (!isPluginModuleEnabled(descriptor.getCompleteKey()))
            {
                if (log.isDebugEnabled())
                    log.debug("Plugin is not enabled, so not enabling ModuleDescriptor '" + descriptor.getName() + "'.");

                continue;
            }

            try
            {
                System.out.println("Enabling " + descriptor.getKey());
                ((StateAware) descriptor).enabled();
            }
            /**
             * Catch any exceptions thrown during the enabling of the plugin (PLUG-7)
             *
             * When a problem occurs, we should catch the exception and make an UnloadablePlugin.
             */
            catch (Exception t)
            {
                log.error("There was an error loading the descriptor '" + descriptor.getName() + "' of plugin '" + plugin.getKey() + "'. Disabling.", t);

                UnloadableModuleDescriptor unloadableDescriptor = UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin, descriptor, t);

                UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin, unloadableDescriptor);

                // Replace the plugin
                replacePluginWithUnloadablePlugin(plugin, unloadablePlugin);
            }
        }
    }

    public void disablePlugin(String key)
    {
        if (key == null)
            throw new IllegalArgumentException("You must specify a plugin key to disable.");

        if (!plugins.containsKey(key))
        {
            if (log.isInfoEnabled())
                log.info("No plugin was found for key '" + key + "'. Not disabling.");

            return;
        }

        Plugin plugin = (Plugin) plugins.get(key);

        notifyPluginDisabled(plugin, getEnabledStateAwareModuleKeys(plugin));
        PluginManagerState currentState = getState();
         if (plugin.isEnabledByDefault())
            currentState.setState(key, Boolean.FALSE);
         else
            currentState.removeState(key);
        saveState(currentState);
    }

    protected List getEnabledStateAwareModuleKeys(Plugin plugin)
    {
        List keys = new ArrayList();
        List moduleDescriptors = new ArrayList(plugin.getModuleDescriptors());
        Collections.reverse(moduleDescriptors);
        for (Iterator it = moduleDescriptors.iterator(); it.hasNext();)
        {
            ModuleDescriptor md = (ModuleDescriptor) it.next();
            if (md instanceof StateAware)
            {
                if (isPluginModuleEnabled(md.getCompleteKey()))
                {
                    keys.add(md.getCompleteKey());
                }
            }
        }
        return keys;
    }

    protected void notifyPluginDisabled(Plugin plugin, List keysToDisable)
    {
        for (Iterator it = keysToDisable.iterator(); it.hasNext();)
        {
            String key = (String)it.next();
            StateAware descriptor = (StateAware)getPluginModule(key);
            descriptor.disabled();
        }
    }

    public void disablePluginModule(String completeKey)
    {
        if (completeKey == null)
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");

        final ModuleDescriptor module = getPluginModule(completeKey);

        if (module == null)
        {
            if (log.isInfoEnabled())
                log.info("Returned module for key '" + completeKey + "' was null. Not disabling.");

            return;
        }
        PluginManagerState currentState = getState();

        if (module.isEnabledByDefault())
            currentState.setState(completeKey, Boolean.FALSE);
        else
            currentState.removeState(completeKey);
        saveState(currentState);
        notifyModuleDisabled(module);
    }

    protected void notifyModuleDisabled(ModuleDescriptor module)
    {
        if (module instanceof StateAware)
            ((StateAware) module).disabled();
    }

    public void enablePluginModule(String completeKey)
    {
        if (completeKey == null)
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");

        final ModuleDescriptor module = getPluginModule(completeKey);

        if (module == null)
        {
            if (log.isInfoEnabled())
                log.info("Returned module for key '" + completeKey + "' was null. Not enabling.");

            return;
        }

        if (!module.satisfiesMinJavaVersion())
        {
            log.error("Minimum Java version of '" + module.getMinJavaVersion() + "' was not satisfied for module '" + completeKey + "'. Not enabling.");
            return;
        }
        PluginManagerState currentState = getState();
        if (!module.isEnabledByDefault())
            currentState.setState(completeKey, Boolean.TRUE);
        else
            currentState.removeState(completeKey);
        saveState(currentState);
        notifyModuleEnabled(module);
    }

    protected void notifyModuleEnabled(ModuleDescriptor module)
    {
        if (module instanceof StateAware)
            ((StateAware) module).enabled();
    }

    public boolean isPluginModuleEnabled(String completeKey)
    {
        ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        final ModuleDescriptor pluginModule = getPluginModule(completeKey);
        return isPluginEnabled(key.getPluginKey()) && pluginModule != null && getState().isEnabled(pluginModule);
    }

    public boolean isPluginEnabled(String key)
    {
        return plugins.containsKey(key) && getState().isEnabled((Plugin) plugins.get(key));
    }

    public List getEnabledModuleDescriptorsByClass(Class descriptorClazz)
    {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();

            // Skip disabled plugins
            if (!isPluginEnabled(plugin.getKey()))
                continue;

            for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
            {
                ModuleDescriptor module = (ModuleDescriptor) iterator1.next();

                if (descriptorClazz.isInstance(module) && isPluginModuleEnabled(module.getCompleteKey()))
                {
                    result.add(module);
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
        final Class descriptorClazz = moduleDescriptorFactory.getModuleDescriptorClass(type);

        if (descriptorClazz == null)
            throw new IllegalArgumentException("No module descriptor of type: " + type + " found.");

        return getEnabledModuleDescriptorsByClass(descriptorClazz);
    }

    public InputStream getDynamicResourceAsStream(String name)
    {
        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();
            if (plugin.isDynamicallyLoaded() && isPluginEnabled(plugin.getKey()))
            {
                InputStream is = plugin.getResourceAsStream(name);

                if (is != null)
                    return is;
            }
        }

        return null;
    }

    public InputStream getPluginResourceAsStream(String pluginKey, String resourcePath)
    {
        Plugin plugin = getEnabledPlugin(pluginKey);

        if (plugin == null)
        {
            log.error("Attempted to retreive resource " + resourcePath + " for non-existent or inactive plugin " + pluginKey);
            return null;
        }

        return plugin.getResourceAsStream(resourcePath);
    }

    /**
     * Replaces a plugin currently loaded with an UnloadablePlugin.
     *
     * The plugin is also disabled.
     *
     * @param plugin the plugin to be replaced
     * @param unloadablePlugin the plugin to replace it
     */
    protected void replacePluginWithUnloadablePlugin(Plugin plugin, UnloadablePlugin unloadablePlugin)
    {
        unloadablePlugin.setUninstallable(plugin.isUninstallable());
        unloadablePlugin.setDeletable(plugin.isDeleteable());
        plugins.put(plugin.getKey(), unloadablePlugin);

        // Disable it
        PluginManagerState currentState = getState();
        currentState.setState(plugin.getKey(), Boolean.FALSE);
        saveState(currentState);
    }

    public boolean isSystemPlugin(String key)
    {
        Plugin plugin = getPlugin(key);
        return plugin != null && plugin.isSystemPlugin();
    }
}