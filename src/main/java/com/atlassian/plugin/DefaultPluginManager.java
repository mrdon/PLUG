package com.atlassian.plugin;

import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptorFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.impl.UnloadablePluginFactory;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.*;

/**
 * This implementation delegates the initiation and classloading of plugins to a
 * list of {@link PluginLoader}s and records the state of plugins in a {@link PluginStateStore}.
 * <p/>
 * This class is responsible for enabling and disabling plugins and plugin modules and reflecting these
 * state changes in the PluginStateStore.
 * <p/>
 * An interesting quirk in the design is that {@link #installPlugin(PluginJar)} explicitly stores
 * the plugin via a {@link PluginInstaller}, whereas {@link #uninstall(Plugin)} relies on the
 * underlying {@link PluginLoader} to remove the plugin if necessary.
 */
public class DefaultPluginManager implements PluginManager
{
    private static final Log log = LogFactory.getLog(DefaultPluginManager.class);
    private final List pluginLoaders;
    private final PluginStateStore store;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private HashMap plugins = new HashMap();

    /**
     * Factory for retrieving descriptor parsers. Typically overridden for testing.
     */
    private DescriptorParserFactory descriptorParserFactory = new XmlDescriptorParserFactory();

    /**
     * Installer used for storing plugins. Used by {@link #installPlugin(PluginJar)}.
     */
    private PluginInstaller pluginInstaller;

    /**
     * Stores {@link Plugin}s as a key and {@link PluginLoader} as a value.
     */
    private HashMap pluginToPluginLoader = new HashMap();

    public DefaultPluginManager(PluginStateStore store, List pluginLoaders, ModuleDescriptorFactory moduleDescriptorFactory)
    {
        this.pluginLoaders = pluginLoaders;
        this.store = store;
        this.moduleDescriptorFactory = moduleDescriptorFactory;
    }

    /**
     * Initialize all plugins in all loaders
     *
     * @throws PluginParseException
     */
    public void init() throws PluginParseException
    {
        for (Iterator iterator = pluginLoaders.iterator(); iterator.hasNext();)
        {
            PluginLoader loader = (PluginLoader) iterator.next();
            if (loader == null) continue;

            for (Iterator pluginIterator = loader.loadAllPlugins(moduleDescriptorFactory).iterator(); pluginIterator.hasNext();)
            {
                addPlugin(loader, (Plugin) pluginIterator.next());
            }
        }
    }

    /**
     * Set the plugin installation strategy for this manager
     * @see PluginInstaller
     * @param pluginInstaller
     */
    public void setPluginInstaller(PluginInstaller pluginInstaller)
    {
        this.pluginInstaller = pluginInstaller;
    }

    protected final PluginStateStore getStore()
    {
        return store;
    }

    public String installPlugin(PluginJar pluginJar) throws PluginParseException
    {
        validatePlugin(pluginJar);

        DescriptorParser descriptorParser = descriptorParserFactory.getInstance(
            pluginJar.getFile(PluginManager.PLUGIN_DESCRIPTOR_FILENAME));
        String key = descriptorParser.getKey();

        pluginInstaller.installPlugin(key, pluginJar);
        scanForNewPlugins();

        return key;
    }

    /**
     * Validate a plugin jar
     * @param pluginJar
     * @throws PluginParseException
     */
    private void validatePlugin(PluginJar pluginJar) throws PluginParseException
    {
        InputStream descriptorStream = pluginJar.getFile(PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
        if (descriptorParser.getKey() == null)
        {
            throw new PluginParseException("Plugin key could not be found in " + PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
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
        unloadPlugin(plugin);

        // PLUG-13: Plugins should not save state across uninstalls.
        removeStateFromStore(getStore(), plugin);
    }

    protected void removeStateFromStore(PluginStateStore stateStore, Plugin plugin)
    {
        PluginManagerState currentState = stateStore.loadPluginState();
        currentState.removeState(plugin.getKey());
        stateStore.savePluginState(currentState);
    }

    /**
     * Unload a plugin. Called when plugins are added locally,
     * or remotely in a clustered application.
     */
    protected void unloadPlugin(Plugin plugin)
        throws PluginException
    {
        if (!plugin.isUninstallable())
            throw new PluginException("Plugin is not uninstallable: " + plugin.getKey());

        PluginLoader loader = (PluginLoader) pluginToPluginLoader.get(plugin);

        if (loader != null && !loader.supportsRemoval())
        {
            throw new PluginException("Not uninstalling plugin - loader doesn't allow removal. Plugin: " + plugin.getKey());
        }

        if (isPluginEnabled(plugin.getKey()))
            notifyPluginDisabled(plugin);

        notifyUninstallPlugin(plugin);
        if (loader != null)
        {
            removePluginFromLoader(plugin);
        }

        plugins.remove(plugin.getKey());
    }

    private void removePluginFromLoader(Plugin plugin)
        throws PluginException
    {
        if (plugin.isDeleteable())
        {
            PluginLoader pluginLoader = (PluginLoader) pluginToPluginLoader.get(plugin);
            pluginLoader.removePlugin(plugin);
        }

        pluginToPluginLoader.remove(plugin);
    }

    protected void notifyUninstallPlugin(Plugin plugin)
    {
        for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) it.next();
            descriptor.destroy(plugin);
        }
    }

    protected PluginManagerState getState()
    {
        return getStore().loadPluginState();
    }

    /**
     * Update the local plugin state and enable state aware modules
     * @param loader
     * @param plugin
     * @throws PluginParseException
     */
    protected void addPlugin(PluginLoader loader, Plugin plugin) throws PluginParseException
    {
        // testing to make sure plugin keys are unique

        if (plugins.containsKey(plugin.getKey())) {
          Plugin existingPlugin = (Plugin) plugins.get(plugin.getKey());
          if(plugin.compareTo(existingPlugin) > 0 ) {
              if (log.isDebugEnabled())
                  log.debug("We found a newer '" + plugin.getKey() + "'");
              try
              {
                  log.info("Unloading " + existingPlugin.getName() + " to upgrade.");
                  updatePlugin(existingPlugin, plugin);
                  if (log.isDebugEnabled())
                      log.debug("Older '" + plugin.getKey() + "' unloaded.");
              } catch (PluginException e) {
                  throw new PluginParseException("Duplicate plugin found (installed version is older) and could not be unloaded: '" + plugin.getKey() + "'");
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

        enablePluginModules(plugin);

        pluginToPluginLoader.put(plugin, loader);
    }

    /**
     * Replace an already loaded plugin with another version. Relevant stored configuration for the plugin will be
     * preserved.
     * @param oldPlugin Plugin to replace
     * @param newPlugin New plugin to install
     * @throws PluginException
     */
    protected void updatePlugin(final Plugin oldPlugin, final Plugin newPlugin) throws PluginException
    {
        if (!oldPlugin.getKey().equals(newPlugin.getKey()))
            throw new IllegalArgumentException("New plugin must have the same key as the old plugin");

        // Preserve the old plugin configuration - uninstall changes it (as disable is called on all modules) and then
        // removes it
        Map oldPluginState = getState().getPluginStateMap(oldPlugin);
        
        uninstall(oldPlugin);

        // Build a set of module keys from the new plugin version
        final Set newModuleKeys = new HashSet();
        newModuleKeys.add(newPlugin.getKey());

        for (Iterator moduleIter = newPlugin.getModuleDescriptors().iterator(); moduleIter.hasNext(); )
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) moduleIter.next();
            newModuleKeys.add(moduleDescriptor.getKey());
        }

        // Remove any keys from the old plugin state that do not exist in the new version
        CollectionUtils.filter(oldPluginState.keySet(), new Predicate() {
            public boolean evaluate(Object object)
            {
                String key = (String) object;
                return newModuleKeys.contains(key);
            }
        });

        // Restore the configuration
        PluginManagerState currentState = getState();
        currentState.getMap().putAll(oldPluginState);
        getStore().savePluginState(currentState);
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

            for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
            {
                ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator1.next();

                final Class moduleDescClass = moduleDescriptor.getModuleClass();
                if (moduleDescClass != null && moduleClass.isAssignableFrom(moduleDescClass) && isPluginModuleEnabled(moduleDescriptor.getCompleteKey()))
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

    public List getEnabledModulesByClassAndDescriptor(Class[] descriptorClazz, Class moduleClass)
    {
        List result = new LinkedList();

        for (Iterator iterator = plugins.values().iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();
            for (Iterator iterator1 = plugin.getModuleDescriptors().iterator(); iterator1.hasNext();)
            {
                ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator1.next();
                if (ArrayUtils.contains(descriptorClazz, moduleDescriptor.getClass()))
                {
                    final Class moduleDescClass = moduleDescriptor.getModuleClass();
                    if (moduleDescClass != null && moduleClass.isAssignableFrom(moduleDescClass) && isPluginModuleEnabled(moduleDescriptor.getCompleteKey()))
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
                            log.error("Unable to load module class " + moduleDescClass, e);
                        }
                    }
                }
            }
        }

        return result;
    }

    public List getEnabledModulesByClassAndDescriptor(Class descriptorClazz, Class moduleClass)
    {
        return getEnabledModulesByClassAndDescriptor(new Class[] {descriptorClazz}, moduleClass);
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

        enablePluginState(plugin, getStore());
        notifyPluginEnabled(plugin);
    }

    protected void enablePluginState(Plugin plugin, PluginStateStore stateStore)
    {
        PluginManagerState currentState = stateStore.loadPluginState();
        String key = plugin.getKey();
        if (!plugin.isEnabledByDefault())
            currentState.setState(key, Boolean.TRUE);
        else
            currentState.removeState(key);
        stateStore.savePluginState(currentState);
    }

    /**
     * Called on all clustered application nodes, rather than {@link #enablePlugin(String)}
     * to just update the local state, state aware modules and loaders, but not affect the
     * global plugin state.
     */
    protected void notifyPluginEnabled(Plugin plugin)
    {
        enablePluginModules(plugin);
    }

    /**
     * For each module in the plugin, call the module descriptor's enabled() method if the module is StateAware and enabled.
     * @param plugin
     */
    private void enablePluginModules(Plugin plugin)
    {
        for (Iterator it = plugin.getModuleDescriptors().iterator(); it.hasNext();)
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
                    log.debug("Plugin module is disabled, so not enabling ModuleDescriptor '" + descriptor.getName() + "'.");
                continue;
            }

            try
            {
                if (log.isDebugEnabled())
                    log.debug("Enabling " + descriptor.getKey());
                ((StateAware) descriptor).enabled();
            }
            catch (Throwable exception) // catch any errors and insert an UnloadablePlugin (PLUG-7)
            {
                log.error("There was an error loading the descriptor '" + descriptor.getName() + "' of plugin '" + plugin.getKey() + "'. Disabling.", exception);
                replacePluginWithUnloadablePlugin(plugin, descriptor, exception);
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

        notifyPluginDisabled(plugin);
        disablePluginState(plugin, getStore());
    }

    protected void disablePluginState(Plugin plugin, PluginStateStore stateStore)
    {
        String key = plugin.getKey();
        PluginManagerState currentState = stateStore.loadPluginState();
        if (plugin.isEnabledByDefault())
            currentState.setState(key, Boolean.FALSE);
        else
            currentState.removeState(key);
        stateStore.savePluginState(currentState);
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

    protected void notifyPluginDisabled(Plugin plugin)
    {
        List keysToDisable = getEnabledStateAwareModuleKeys(plugin);
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
        disablePluginModuleState(module, getStore());
        notifyModuleDisabled(module);
    }

    protected void disablePluginModuleState(ModuleDescriptor module, PluginStateStore stateStore)
    {
        String completeKey = module.getCompleteKey();
        PluginManagerState currentState = stateStore.loadPluginState();
        if (module.isEnabledByDefault())
            currentState.setState(completeKey, Boolean.FALSE);
        else
            currentState.removeState(completeKey);
        stateStore.savePluginState(currentState);
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
        enablePluginModuleState(module, getStore());
        notifyModuleEnabled(module);
    }

    protected void enablePluginModuleState(ModuleDescriptor module, PluginStateStore stateStore)
    {
        String completeKey = module.getCompleteKey();
        PluginManagerState currentState = stateStore.loadPluginState();
        if (!module.isEnabledByDefault())
            currentState.setState(completeKey, Boolean.TRUE);
        else
            currentState.removeState(completeKey);
        stateStore.savePluginState(currentState);
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
     * Disables and replaces a plugin currently loaded with an UnloadablePlugin.
     *
     * @param plugin the plugin to be replaced
     * @param descriptor the descriptor which caused the problem
     * @param throwable the problem caught when enabling the descriptor
     * @return the UnloadablePlugin which replaced the broken plugin
     */
    private UnloadablePlugin replacePluginWithUnloadablePlugin(Plugin plugin, ModuleDescriptor descriptor, Throwable throwable)
    {
        UnloadableModuleDescriptor unloadableDescriptor =
            UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin, descriptor, throwable);
        UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin, unloadableDescriptor);

        unloadablePlugin.setUninstallable(plugin.isUninstallable());
        unloadablePlugin.setDeletable(plugin.isDeleteable());
        plugins.put(plugin.getKey(), unloadablePlugin);

        // Disable it
        disablePluginState(plugin, getStore());
        return unloadablePlugin;
    }

    public boolean isSystemPlugin(String key)
    {
        Plugin plugin = getPlugin(key);
        return plugin != null && plugin.isSystemPlugin();
    }

    public void setDescriptorParserFactory(DescriptorParserFactory descriptorParserFactory)
    {
        this.descriptorParserFactory = descriptorParserFactory;
    }
}