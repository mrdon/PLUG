package com.atlassian.plugin.manager;

import com.atlassian.plugin.*;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptorFactory;
import com.atlassian.plugin.predicate.*;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.WaitUntil;
import static com.atlassian.plugin.util.collect.CollectionUtil.toList;
import static com.atlassian.plugin.util.collect.CollectionUtil.filter;
import static com.atlassian.plugin.util.collect.CollectionUtil.transform;
import com.atlassian.plugin.util.collect.Predicate;
import com.atlassian.plugin.util.collect.Function;
import com.atlassian.plugin.util.collect.CollectionUtil;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.impl.UnloadablePluginFactory;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.events.*;
import com.atlassian.plugin.classloader.PluginsClassLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.DynamicPluginLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.InputStream;

/**
 * This implementation delegates the initiation and classloading of plugins to a
 * list of {@link com.atlassian.plugin.loaders.PluginLoader}s and records the state of plugins in a {@link com.atlassian.plugin.manager.PluginPersistentStateStore}.
 * <p/>
 * This class is responsible for enabling and disabling plugins and plugin modules and reflecting these
 * state changes in the PluginPersistentStateStore.
 * <p/>
 * An interesting quirk in the design is that {@link #installPlugin(com.atlassian.plugin.PluginArtifact)} explicitly stores
 * the plugin via a {@link com.atlassian.plugin.PluginInstaller}, whereas {@link #uninstall(com.atlassian.plugin.Plugin)} relies on the
 * underlying {@link com.atlassian.plugin.loaders.PluginLoader} to remove the plugin if necessary.
 */
public class DefaultPluginManager implements PluginController, PluginAccessor, PluginSystemLifecycle
{
    private static final Log log = LogFactory.getLog(DefaultPluginManager.class);

    private final List<PluginLoader> pluginLoaders;
    private final PluginPersistentStateStore store;
    private final ModuleDescriptorFactory moduleDescriptorFactory;
    private final PluginsClassLoader classLoader;
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<String, Plugin>();
    private final PluginEventManager pluginEventManager;

    private final StateTracker tracker = new StateTracker();

    /**
     * Installer used for storing plugins. Used by {@link #installPlugin(PluginArtifact)}.
     */
    private PluginInstaller pluginInstaller;

    /**
     * Stores {@link Plugin}s as a key and {@link PluginLoader} as a value.
     */
    private final Map<Plugin, PluginLoader> pluginToPluginLoader = new HashMap<Plugin, PluginLoader>();

    public DefaultPluginManager(final PluginPersistentStateStore store, final List<PluginLoader> pluginLoaders, final ModuleDescriptorFactory moduleDescriptorFactory, final PluginEventManager pluginEventManager)
    {
        if (store == null)
        {
            throw new IllegalArgumentException("PluginPersistentStateStore must not be null.");
        }
        if (pluginLoaders == null)
        {
            throw new IllegalArgumentException("Plugin Loaders list must not be null.");
        }
        if (moduleDescriptorFactory == null)
        {
            throw new IllegalArgumentException("ModuleDescriptorFactory must not be null.");
        }
        if (pluginEventManager == null)
        {
            throw new IllegalArgumentException("PluginEventManager must not be null.");
        }
        this.pluginLoaders = pluginLoaders;
        this.store = store;
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        this.pluginEventManager = pluginEventManager;
        this.pluginEventManager.register(this);
        classLoader = new PluginsClassLoader(this);
    }

    /**
     * Initialize all plugins in all loaders
     *
     * @throws PluginParseException
     * @throws IllegalStateException if already initialized or already in the process of initialization.
     */
    public void init() throws PluginParseException
    {
        tracker.setState(StateTracker.State.STARTING);
        final long start = System.currentTimeMillis();
        log.info("Initialising the plugin system");
        pluginEventManager.broadcast(new PluginFrameworkStartingEvent(this, this));
        for (final PluginLoader loader : pluginLoaders)
        {
            if (loader == null)
            {
                continue;
            }

            Collection<Plugin> possiblePluginsToLoad = loader.loadAllPlugins(moduleDescriptorFactory);
            Collection<Plugin> pluginsToLoad = new ArrayList<Plugin>();
            for (Iterator<Plugin> i = possiblePluginsToLoad.iterator(); i.hasNext(); )
            {
                Plugin plugin = i.next();
                if (getState().getPluginRestartState(plugin.getKey()) == PluginRestartState.REMOVE)
                {
                    loader.removePlugin(plugin);

                    // PLUG-13: Plugins should not save state across uninstalls.
                    removeStateFromStore(getStore(), plugin);
                }
                else
                {
                    pluginsToLoad.add(plugin);
                }
            }
            addPlugins(loader, pluginsToLoad);
        }

        DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
        currentState.clearPluginRestartState();
        getStore().save(currentState);

        pluginEventManager.broadcast(new PluginFrameworkStartedEvent(this, this));
        final long end = System.currentTimeMillis();
        log.info("Plugin system started in " + (end - start) + "ms");
        tracker.setState(StateTracker.State.STARTED);
    }

    /**
     * Fires the shutdown event
     * @since 2.0.0
     * @throws IllegalStateException if already shutdown or already in the process of shutting down.
     */
    public void shutdown()
    {
        tracker.setState(StateTracker.State.SHUTTING_DOWN);
        log.info("Shutting down the plugin system");
        pluginEventManager.broadcast(new PluginFrameworkShutdownEvent(this, this));
        tracker.setState(StateTracker.State.SHUTDOWN);
        plugins.clear();
        pluginEventManager.unregister(this);
    }

    @PluginEventListener
    public void onPluginRefresh(final PluginRefreshedEvent event)
    {
        final Plugin plugin = event.getPlugin();

        // disable the plugin, shamefully copied from notifyPluginDisabled()
        final List<ModuleDescriptor<?>> moduleDescriptors = new ArrayList<ModuleDescriptor<?>>(plugin.getModuleDescriptors());
        Collections.reverse(moduleDescriptors); // disable in reverse order

        for (final ModuleDescriptor<?> module : moduleDescriptors)
        {
            // don't actually disable the module, just fire the events because its plugin is being disabled
            // if the module was actually disabled, you'd have to reenable each one when enabling the plugin

            if (isPluginModuleEnabled(module.getCompleteKey()))
            {
                publishModuleDisabledEvents(module);
            }
        }

        // enable the plugin, shamefully copied from notifyPluginEnabled()
        classLoader.notifyPluginOrModuleEnabled();
        enablePluginModules(plugin);
        pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
    }

    /**
     * Set the plugin installation strategy for this manager
     *
     * @param pluginInstaller the plugin installation strategy to use
     * @see PluginInstaller
     */
    public void setPluginInstaller(final PluginInstaller pluginInstaller)
    {
        this.pluginInstaller = pluginInstaller;
    }

    protected final PluginPersistentStateStore getStore()
    {
        return store;
    }

    public String installPlugin(final PluginArtifact pluginArtifact) throws PluginParseException
    {
        final String key = validatePlugin(pluginArtifact);
        pluginInstaller.installPlugin(key, pluginArtifact);
        scanForNewPlugins();
        return key;
    }

    /**
     * Validate a plugin jar.  Looks through all plugin loaders for ones that can load the plugin and
     * extract the plugin key as proof.
     *
     * @param pluginArtifact the jar file representing the plugin
     * @return The plugin key
     * @throws PluginParseException if the plugin cannot be parsed
     * @throws NullPointerException if <code>pluginJar</code> is null.
     */
    String validatePlugin(final PluginArtifact pluginArtifact) throws PluginParseException
    {
        boolean foundADynamicPluginLoader = false;
        for (final PluginLoader loader : pluginLoaders)
        {
            if (loader instanceof DynamicPluginLoader)
            {
                foundADynamicPluginLoader = true;
                final String key = ((DynamicPluginLoader) loader).canLoad(pluginArtifact);
                if (key != null)
                {
                    return key;
                }
            }
        }

        if (!foundADynamicPluginLoader)
        {
            throw new IllegalStateException("Should be at least one DynamicPluginLoader in the plugin loader list");
        }
        throw new PluginParseException("Jar " + pluginArtifact.getName() + " is not a valid plugin");
    }

    public int scanForNewPlugins() throws PluginParseException
    {
        int numberFound = 0;

        for (final PluginLoader loader : pluginLoaders)
        {
            if (loader != null)
            {
                if (loader.supportsAddition())
                {
                    final List<Plugin> pluginsToAdd = new ArrayList<Plugin>();
                    for (Plugin plugin : loader.addFoundPlugins(moduleDescriptorFactory))
                    {
                        Plugin oldPlugin = plugins.get(plugin.getKey());
                        // Only actually install the plugin if its module descriptors support it.  Otherwise, mark it as
                        // unloadable.
                        if (!(plugin instanceof UnloadablePlugin))
                        {
                            if (PluginUtils.doesPluginRequireRestart(plugin))
                            {
                                if (oldPlugin == null)
                                {
                                    DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
                                    currentState.setPluginRestartState(plugin.getKey(), PluginRestartState.INSTALL);
                                    getStore().save(currentState);

                                    final UnloadablePlugin unloadablePlugin = new UnloadablePlugin("Plugin requires a restart of the application");
                                    unloadablePlugin.setKey(plugin.getKey());
                                    plugin = unloadablePlugin;
                                }
                                else
                                {
                                    DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
                                    currentState.setPluginRestartState(plugin.getKey(), PluginRestartState.UPGRADE);
                                    getStore().save(currentState);
                                    continue;
                                }
                            }

                            // Check to ensure that the old plugin didn't require restart, even if the new one doesn't
                            else if (oldPlugin != null && PluginUtils.doesPluginRequireRestart(oldPlugin))
                            {
                                DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
                                currentState.setPluginRestartState(plugin.getKey(), PluginRestartState.UPGRADE);
                                getStore().save(currentState);
                                continue;
                            }
                            pluginsToAdd.add(plugin);
                        }

                    }
                    addPlugins(loader, pluginsToAdd);
                    numberFound = pluginsToAdd.size();
                }
            }
        }

        return numberFound;
    }

    /**
     *
     * @param plugin
     * @throws PluginException If the plugin or loader doesn't support uninstallation
     */
    public void uninstall(final Plugin plugin) throws PluginException
    {
        if (PluginUtils.doesPluginRequireRestart(plugin))
        {
            ensurePluginAndLoaderSupportsUninstall(plugin);
            DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
            currentState.setPluginRestartState(plugin.getKey(), PluginRestartState.REMOVE);
            getStore().save(currentState);
        }
        else
        {
            unloadPlugin(plugin);

            // PLUG-13: Plugins should not save state across uninstalls.
            removeStateFromStore(getStore(), plugin);
        }
    }

    protected void removeStateFromStore(final PluginPersistentStateStore stateStore, final Plugin plugin)
    {
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(stateStore.load());
        currentState.removeState(plugin.getKey());
        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors())
        {
            currentState.removeState(moduleDescriptor.getCompleteKey());
        }
        stateStore.save(currentState);
    }

    /**
     * Unload a plugin. Called when plugins are added locally,
     * or remotely in a clustered application.
     *
     * @param plugin the plugin to remove
     * @throws PluginException if th eplugin cannot be uninstalled
     */
    protected void unloadPlugin(final Plugin plugin) throws PluginException
    {
        final PluginLoader loader = ensurePluginAndLoaderSupportsUninstall(plugin);

        if (isPluginEnabled(plugin.getKey()))
        {
            notifyPluginDisabled(plugin);
        }

        notifyUninstallPlugin(plugin);
        if (loader != null)
        {
            removePluginFromLoader(plugin);
        }

        plugins.remove(plugin.getKey());
    }

    private PluginLoader ensurePluginAndLoaderSupportsUninstall(Plugin plugin)
    {
        if (!plugin.isUninstallable())
        {
            throw new PluginException("Plugin is not uninstallable: " + plugin.getKey());
        }

        final PluginLoader loader = pluginToPluginLoader.get(plugin);

        if ((loader != null) && !loader.supportsRemoval())
        {
            throw new PluginException("Not uninstalling plugin - loader doesn't allow removal. Plugin: " + plugin.getKey());
        }
        return loader;
    }

    private void removePluginFromLoader(final Plugin plugin) throws PluginException
    {
        if (plugin.isDeleteable())
        {
            final PluginLoader pluginLoader = pluginToPluginLoader.get(plugin);
            pluginLoader.removePlugin(plugin);
        }

        pluginToPluginLoader.remove(plugin);
    }

    protected void notifyUninstallPlugin(final Plugin plugin)
    {
        classLoader.notifyUninstallPlugin(plugin);

        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors())
        {
            descriptor.destroy(plugin);
        }
    }

    protected PluginPersistentState getState()
    {
        return getStore().load();
    }

    /**
     * @deprecated Since 2.0.2, use {@link #addPlugins(PluginLoader,Collection<Plugin>...)} instead
     */
    @Deprecated
    protected void addPlugin(final PluginLoader loader, final Plugin plugin) throws PluginParseException
    {
        addPlugins(loader, Collections.singletonList(plugin));
    }

    /**
     * Update the local plugin state and enable state aware modules.
     * <p>
     * If there is an existing plugin with the same key, the version strings of the existing plugin and the plugin
     * provided to this method will be parsed and compared.  If the installed version is newer than the provided
     * version, it will not be changed.  If the specified plugin's version is the same or newer, the existing plugin
     * state will be saved and the plugin will be unloaded before the provided plugin is installed.  If the existing
     * plugin cannot be unloaded a {@link PluginException} will be thrown.
     *
     * @param loader the loader used to load this plugin
     * @param pluginsToAdd the plugins to add
     * @throws PluginParseException if the plugin cannot be parsed
     * @since 2.0.2
     */
    protected void addPlugins(final PluginLoader loader, final Collection<Plugin> pluginsToAdd) throws PluginParseException
    {
        Set<Plugin> installedPlugins = new TreeSet<Plugin>();
        for (final Plugin plugin : new TreeSet<Plugin>(pluginsToAdd))
        {
            boolean pluginUpgraded = false;
            // testing to make sure plugin keys are unique
            if (plugins.containsKey(plugin.getKey()))
            {
                final Plugin existingPlugin = plugins.get(plugin.getKey());
                if (plugin.compareTo(existingPlugin) >= 0)
                {
                    try
                    {
                        updatePlugin(existingPlugin, plugin);
                        pluginUpgraded = true;
                    }
                    catch (final PluginException e)
                    {
                        throw new PluginParseException(
                            "Duplicate plugin found (installed version is the same or older) and could not be unloaded: '" + plugin.getKey() + "'", e);
                    }
                }
                else
                {
                    // If we find an older plugin, don't error, just ignore it. PLUG-12.
                    if (log.isDebugEnabled())
                    {
                        log.debug("Duplicate plugin found (installed version is newer): '" + plugin.getKey() + "'");
                    }
                    // and don't install the older plugin
                    continue;
                }
            }
            plugin.install();
            installedPlugins.add(plugin);
            if (pluginUpgraded)
            {
                pluginEventManager.broadcast(new PluginUpgradedEvent(plugin));
            }
        }

        final Set<Plugin> pluginsInEnablingState = new HashSet<Plugin>();
        for (final Plugin plugin : installedPlugins)
        {
            plugins.put(plugin.getKey(), plugin);
            if (getState().isEnabled(plugin))
            {
                try
                {
                    plugin.enable();
                    if (plugin.getPluginState() == PluginState.ENABLING)
                    {
                        pluginsInEnablingState.add(plugin);
                    }
                }
                catch (final RuntimeException ex)
                {
                    log.error("Unable to enable plugin " + plugin.getKey(), ex);
                }
            }

            pluginToPluginLoader.put(plugin, loader);
        }

        if (!plugins.isEmpty())
        {
            // Now try to enable plugins that weren't enabled before, probably due to dependency ordering issues
            WaitUntil.invoke(new WaitUntil.WaitCondition()
            {
                public boolean isFinished()
                {
                    for (final Iterator<Plugin> i = pluginsInEnablingState.iterator(); i.hasNext();)
                    {
                        final Plugin plugin = i.next();
                        if (plugin.getPluginState() != PluginState.ENABLING)
                        {
                            i.remove();
                        }
                    }
                    return pluginsInEnablingState.isEmpty();
                }

                public String getWaitMessage()
                {
                    return "Plugins that have yet to start: " + pluginsInEnablingState;
                }
            }, 60);

            // Disable any plugins that aren't enabled by now
            if (!pluginsInEnablingState.isEmpty())
            {
                final StringBuilder sb = new StringBuilder();
                for (final Plugin plugin : pluginsInEnablingState)
                {
                    sb.append(plugin.getKey()).append(',');
                    disablePlugin(plugin.getKey());
                }
                sb.deleteCharAt(sb.length() - 1);
                log.error("Unable to start the following plugins: " + sb.toString());
            }
        }

        for (final Plugin plugin : pluginsToAdd)
        {
            if (plugin.getPluginState() == PluginState.ENABLED)
            {
                // This method enables the plugin modules
                enablePluginModules(plugin);
                pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
            }
        }
    }

    /**
     * Replace an already loaded plugin with another version. Relevant stored configuration for the plugin will be
     * preserved.
     *
     * @param oldPlugin Plugin to replace
     * @param newPlugin New plugin to install
     * @throws PluginException if the plugin cannot be updated
     */
    protected void updatePlugin(final Plugin oldPlugin, final Plugin newPlugin) throws PluginException
    {
        if (!oldPlugin.getKey().equals(newPlugin.getKey()))
        {
            throw new IllegalArgumentException("New plugin must have the same key as the old plugin");
        }

        if (log.isInfoEnabled())
        {
            log.info("Updating plugin '" + oldPlugin + "' to '" + newPlugin + "'");
        }

        // Preserve the old plugin configuration - uninstall changes it (as disable is called on all modules) and then
        // removes it
        final Map<String, Boolean> oldPluginState = new HashMap<String, Boolean>(getState().getPluginStateMap(oldPlugin));

        if (log.isDebugEnabled())
        {
            log.debug("Uninstalling old plugin: " + oldPlugin);
        }
        uninstall(oldPlugin);
        if (log.isDebugEnabled())
        {
            log.debug("Plugin uninstalled '" + oldPlugin + "', preserving old state");
        }

        // Build a set of module keys from the new plugin version
        final Set<String> newModuleKeys = new HashSet<String>();
        newModuleKeys.add(newPlugin.getKey());

        for (final ModuleDescriptor<?> moduleDescriptor : newPlugin.getModuleDescriptors())
        {
            newModuleKeys.add(moduleDescriptor.getCompleteKey());
        }

        // Remove any keys from the old plugin state that do not exist in the new version
        CollectionUtils.filter(oldPluginState.keySet(), new org.apache.commons.collections.Predicate()
        {
            public boolean evaluate(final Object o)
            {
                return newModuleKeys.contains(o);
            }
        });

        // Restore the configuration
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(getStore().load());
        currentState.addState(oldPluginState);
        getStore().save(currentState);
    }

    public Collection<Plugin> getPlugins()
    {
        return plugins.values();
    }

    /**
     * @see PluginAccessor#getPlugins(com.atlassian.plugin.predicate.PluginPredicate)
     * @since 0.17
     */
    public Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate)
    {
        return toList(filter(getPlugins(), new Predicate<Plugin>()
        {
            public boolean evaluate(final Plugin plugin)
            {
                return pluginPredicate.matches(plugin);
            }
        }));
    }

    /**
     * @see PluginAccessor#getEnabledPlugins()
     */
    public Collection<Plugin> getEnabledPlugins()
    {
        return getPlugins(new EnabledPluginPredicate(this));
    }

    /**
     * @see PluginAccessor#getModules(com.atlassian.plugin.predicate.ModuleDescriptorPredicate)
     * @since 0.17
     */
    public <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        final Collection<ModuleDescriptor<M>> moduleDescriptors = getModuleDescriptors(moduleDescriptorPredicate);
        return getModules(moduleDescriptors);
    }

    /**
     * @see PluginAccessor#getModuleDescriptors(com.atlassian.plugin.predicate.ModuleDescriptorPredicate)
     * @since 0.17
     */
    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        final List<ModuleDescriptor<M>> moduleDescriptors = getModuleDescriptorsList(getPlugins());
        return toList(filter(moduleDescriptors, new Predicate<ModuleDescriptor<M>>()
        {
            public boolean evaluate(final ModuleDescriptor<M> input)
            {
                return moduleDescriptorPredicate.matches(input);
            }
        }));
    }

    /**
     * Get the all the module descriptors from the given collection of plugins.
     * <p>
     * Be careful, this does not actually return a list of ModuleDescriptors that are M, it returns all
     * ModuleDescriptors of all types, you must further filter the list as required.
     *
     * @param plugins a collection of {@link Plugin}s
     * @return a collection of {@link ModuleDescriptor}s
     */
    private <M> List<ModuleDescriptor<M>> getModuleDescriptorsList(final Collection<Plugin> plugins)
    {
        // hack way to get typed descriptors from plugin and keep generics happy
        final List<ModuleDescriptor<M>> moduleDescriptors = new LinkedList<ModuleDescriptor<M>>();
        for (final Plugin plugin : plugins)
        {
            final Collection<ModuleDescriptor<?>> descriptors = plugin.getModuleDescriptors();
            for (final ModuleDescriptor<?> moduleDescriptor : descriptors)
            {
                @SuppressWarnings("unchecked")
                final ModuleDescriptor<M> typedDescriptor = (ModuleDescriptor<M>) moduleDescriptor;
                moduleDescriptors.add(typedDescriptor);
            }
        }
        return moduleDescriptors;
    }

    /**
     * Get the modules of all the given descriptor.  If any of the getModule() calls fails, the error is recorded in
     * the logs and the plugin is disabled.
     *
     * @param moduleDescriptors the collection of module descriptors to get the modules from.
     * @return a {@link Collection} modules that can be any type of object.
     *         This collection will not contain any null value.
     */
    private <M> List<M> getModules(final Iterable<ModuleDescriptor<M>> moduleDescriptors)
    {
        final Set<String> pluginsToDisable = new HashSet<String>();
        List<M> modules = transform(moduleDescriptors, new Function<ModuleDescriptor<M>, M>()
        {
            public M get(final ModuleDescriptor<M> input)
            {
                M result = null;
                try
                {
                    result = input.getModule();
                }
                catch (RuntimeException ex)
                {
                    log.error("Exception when retrieving plugin module "+input.getKey()+", will disable plugin "
                            + input.getPlugin().getKey(), ex);
                    pluginsToDisable.add(input.getPlugin().getKey());
                }
                return result;
            }
        });

        for (String badPluginKey : pluginsToDisable)
        {
            disablePlugin(badPluginKey);
        }
        return modules;
    }

    public Plugin getPlugin(final String key)
    {
        return plugins.get(key);
    }

    public Plugin getEnabledPlugin(final String pluginKey)
    {
        if (!isPluginEnabled(pluginKey))
        {
            return null;
        }
        return getPlugin(pluginKey);
    }

    public ModuleDescriptor<?> getPluginModule(final String completeKey)
    {
        final ModuleCompleteKey key = new ModuleCompleteKey(completeKey);
        final Plugin plugin = getPlugin(key.getPluginKey());

        if (plugin == null)
        {
            return null;
        }
        return plugin.getModuleDescriptor(key.getModuleKey());
    }

    public ModuleDescriptor<?> getEnabledPluginModule(final String completeKey)
    {
        final ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        // If it's disabled, return null
        if (!isPluginModuleEnabled(completeKey))
        {
            return null;
        }

        return getEnabledPlugin(key.getPluginKey()).getModuleDescriptor(key.getModuleKey());
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClass(Class)
     */
    public <M> List<M> getEnabledModulesByClass(final Class<M> moduleClass)
    {
        return getModules(getEnabledModuleDescriptorsByModuleClass(moduleClass));
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClassAndDescriptor(Class[], Class)
     * @deprecated since 0.17, use {@link #getModules(com.atlassian.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>>[] descriptorClasses, final Class<M> moduleClass)
    {
        final Iterable<ModuleDescriptor<M>> moduleDescriptors = filterModuleDescriptors(getEnabledModuleDescriptorsByModuleClass(moduleClass),
            new ModuleDescriptorOfClassPredicate<M>(descriptorClasses));

        return getModules(moduleDescriptors);
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClassAndDescriptor(Class, Class)
     * @deprecated since 0.17, use {@link #getModules(com.atlassian.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass)
    {
        final Iterable<ModuleDescriptor<M>> moduleDescriptors = getEnabledModuleDescriptorsByModuleClass(moduleClass);
        return getModules(filterModuleDescriptors(moduleDescriptors, new ModuleDescriptorOfClassPredicate<M>(descriptorClass)));
    }

    /**
     * Get all module descriptor that are enabled and for which the module is an instance of the given class.
     *
     * @param moduleClass the class of the module within the module descriptor.
     * @return a collection of {@link ModuleDescriptor}s
     */
    private <M> Collection<ModuleDescriptor<M>> getEnabledModuleDescriptorsByModuleClass(final Class<M> moduleClass)
    {
        Iterable<ModuleDescriptor<M>> moduleDescriptors = getModuleDescriptorsList(getEnabledPlugins());
        moduleDescriptors = filterModuleDescriptors(moduleDescriptors, new ModuleOfClassPredicate<M>(moduleClass));
        moduleDescriptors = filterModuleDescriptors(moduleDescriptors, new EnabledModulePredicate<M>(this));

        return toList(moduleDescriptors);
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz)
    {
        return getEnabledModuleDescriptorsByClass(descriptorClazz, false);
    }

    /**
     * This method has been reverted to pre PLUG-40 to fix performance issues that were encountered during
     * load testing. This should be reverted to the state it was in at 54639 when the fundamental issue leading
     * to this slowdown has been corrected (that is, slowness of PluginClassLoader).
     *
     * @see PluginAccessor#getEnabledModuleDescriptorsByClass(Class)
     */
    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz, final boolean verbose)
    {
        final List<D> result = new LinkedList<D>();
        for (final Plugin plugin : plugins.values())
        {
            // Skip disabled plugins
            if (!isPluginEnabled(plugin.getKey()))
            {
                if (verbose && log.isInfoEnabled())
                {
                    log.info("Plugin [" + plugin.getKey() + "] is disabled.");
                }
                continue;
            }

            for (final ModuleDescriptor<?> module : plugin.getModuleDescriptors())
            {
                if (descriptorClazz.isInstance(module) && isPluginModuleEnabled(module.getCompleteKey()))
                {
                    @SuppressWarnings("unchecked")
                    final D moduleDescriptor = (D) module;
                    result.add(moduleDescriptor);
                }
                else
                {
                    if (verbose && log.isInfoEnabled())
                    {
                        log.info("Module [" + module.getCompleteKey() + "] is disabled.");
                    }
                }
            }
        }

        return result;
    }

    /**
     * @see PluginAccessor#getEnabledModuleDescriptorsByType(String)
     * @deprecated since 0.17, use {@link #getModuleDescriptors(com.atlassian.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(final String type) throws PluginParseException, IllegalArgumentException
    {
        Iterable<ModuleDescriptor<M>> moduleDescriptors = getModuleDescriptorsList(getEnabledPlugins());
        moduleDescriptors = filterModuleDescriptors(moduleDescriptors, new ModuleDescriptorOfTypePredicate<M>(moduleDescriptorFactory, type));
        moduleDescriptors = filterModuleDescriptors(moduleDescriptors, new EnabledModulePredicate<M>(this));
        return toList(moduleDescriptors);
    }

    /**
     * Filters out a collection of {@link ModuleDescriptor}s given a predicate.
     *
     * @param moduleDescriptors         the collection of {@link ModuleDescriptor}s to filter.
     * @param moduleDescriptorPredicate the predicate to use for filtering.
     */
    private static <M> Iterable<ModuleDescriptor<M>> filterModuleDescriptors(final Iterable<ModuleDescriptor<M>> moduleDescriptors, final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        return CollectionUtil.filter(moduleDescriptors, new Predicate<ModuleDescriptor<M>>()
        {
            public boolean evaluate(final ModuleDescriptor<M> input)
            {
                return moduleDescriptorPredicate.matches(input);
            }
        });
    }

    public void enablePlugin(final String key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("You must specify a plugin key to disable.");
        }

        if (!plugins.containsKey(key))
        {
            if (log.isInfoEnabled())
            {
                log.info("No plugin was found for key '" + key + "'. Not enabling.");
            }

            return;
        }

        final Plugin plugin = plugins.get(key);

        if (!plugin.getPluginInformation().satisfiesMinJavaVersion())
        {
            log.error("Minimum Java version of '" + plugin.getPluginInformation().getMinJavaVersion() + "' was not satisfied for module '" + key + "'. Not enabling.");
            return;
        }

        plugin.enable();

        // Only change the state if the plugin was enabled successfully
        if (WaitUntil.invoke(new PluginEnabledCondition(plugin)))
        {
            enablePluginState(plugin, getStore());
            notifyPluginEnabled(plugin);
        }
    }

    protected void enablePluginState(final Plugin plugin, final PluginPersistentStateStore stateStore)
    {
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(stateStore.load());
        currentState.setEnabled(plugin, true);
        stateStore.save(currentState);
    }

    /**
     * Called on all clustered application nodes, rather than {@link #enablePlugin(String)}
     * to just update the local state, state aware modules and loaders, but not affect the
     * global plugin state.
     *
     * @param plugin the plugin being enabled
     */
    protected void notifyPluginEnabled(final Plugin plugin)
    {
        plugin.enable();
        classLoader.notifyPluginOrModuleEnabled();
        enablePluginModules(plugin);
        pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
    }

    /**
     * For each module in the plugin, call the module descriptor's enabled() method if the module is StateAware and enabled.
     *
     * @param plugin the plugin to enable
     */
    private void enablePluginModules(final Plugin plugin)
    {
        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors())
        {
            if (!isPluginModuleEnabled(descriptor.getCompleteKey()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Plugin module is disabled, so not enabling ModuleDescriptor '" + descriptor.getName() + "'.");
                }
                continue;
            }

            try
            {
                notifyModuleEnabled(descriptor);
            }
            catch (final Throwable exception) // catch any errors and insert an UnloadablePlugin (PLUG-7)
            {
                log.error("There was an error loading the descriptor '" + descriptor.getName() + "' of plugin '" + plugin.getKey() + "'. Disabling.",
                    exception);
                replacePluginWithUnloadablePlugin(plugin, descriptor, exception);
            }
        }
        classLoader.notifyPluginOrModuleEnabled();
    }

    public void disablePlugin(final String key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("You must specify a plugin key to disable.");
        }

        if (!plugins.containsKey(key))
        {
            if (log.isInfoEnabled())
            {
                log.info("No plugin was found for key '" + key + "'. Not disabling.");
            }

            return;
        }

        final Plugin plugin = plugins.get(key);

        notifyPluginDisabled(plugin);
        disablePluginState(plugin, getStore());
    }

    protected void disablePluginState(final Plugin plugin, final PluginPersistentStateStore stateStore)
    {
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(stateStore.load());
        currentState.setEnabled(plugin, false);
        stateStore.save(currentState);
    }

    protected void notifyPluginDisabled(final Plugin plugin)
    {
        final List<ModuleDescriptor<?>> moduleDescriptors = new ArrayList<ModuleDescriptor<?>>(plugin.getModuleDescriptors());
        Collections.reverse(moduleDescriptors); // disable in reverse order

        for (final ModuleDescriptor<?> module : moduleDescriptors)
        {
            // don't actually disable the module, just fire the events because its plugin is being disabled
            // if the module was actually disabled, you'd have to reenable each one when enabling the plugin

            if (isPluginModuleEnabled(module.getCompleteKey()))
            {
                publishModuleDisabledEvents(module);
            }
        }

        // This needs to happen after modules are disabled to prevent errors
        plugin.disable();
        pluginEventManager.broadcast(new PluginDisabledEvent(plugin));
    }

    public void disablePluginModule(final String completeKey)
    {
        if (completeKey == null)
        {
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");
        }

        final ModuleDescriptor<?> module = getPluginModule(completeKey);

        if (module == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Returned module for key '" + completeKey + "' was null. Not disabling.");
            }

            return;
        }
        disablePluginModuleState(module, getStore());
        notifyModuleDisabled(module);
    }

    protected void disablePluginModuleState(final ModuleDescriptor<?> module, final PluginPersistentStateStore stateStore)
    {
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(stateStore.load());
        currentState.setEnabled(module, false);
        stateStore.save(currentState);
    }

    protected void notifyModuleDisabled(final ModuleDescriptor<?> module)
    {
        publishModuleDisabledEvents(module);
    }

    private void publishModuleDisabledEvents(final ModuleDescriptor<?> module)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Disabling " + module.getKey());
        }

        if (module instanceof StateAware)
        {
            ((StateAware) module).disabled();
        }

        pluginEventManager.broadcast(new PluginModuleDisabledEvent(module));
    }

    public void enablePluginModule(final String completeKey)
    {
        if (completeKey == null)
        {
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");
        }

        final ModuleDescriptor<?> module = getPluginModule(completeKey);

        if (module == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Returned module for key '" + completeKey + "' was null. Not enabling.");
            }

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

    protected void enablePluginModuleState(final ModuleDescriptor<?> module, final PluginPersistentStateStore stateStore)
    {
        final DefaultPluginPersistentState currentState = new DefaultPluginPersistentState(stateStore.load());
        currentState.setEnabled(module, true);
        stateStore.save(currentState);
    }

    protected void notifyModuleEnabled(final ModuleDescriptor<?> module)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Enabling " + module.getKey());
        }
        classLoader.notifyPluginOrModuleEnabled();
        if (module instanceof StateAware)
        {
            ((StateAware) module).enabled();
        }
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(module));
    }

    public boolean isPluginModuleEnabled(final String completeKey)
    {
        // completeKey may be null
        if (completeKey == null)
        {
            return false;
        }
        final ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        final ModuleDescriptor<?> pluginModule = getPluginModule(completeKey);
        return isPluginEnabled(key.getPluginKey()) && (pluginModule != null) && getState().isEnabled(pluginModule);
    }

    /**
     * This method checks to see if the plugin should be enabled based on the state manager and the plugin.
     *
     * @param key The plugin key
     * @return True if the plugin is enabled
     */
    public boolean isPluginEnabled(final String key)
    {
        final Plugin plugin = plugins.get(key);

        return (plugin != null) && getState().isEnabled(plugin) && (plugin.getPluginState() == PluginState.ENABLED);
    }

    public InputStream getDynamicResourceAsStream(final String name)
    {
        return getClassLoader().getResourceAsStream(name);
    }

    public Class<?> getDynamicPluginClass(final String className) throws ClassNotFoundException
    {
        return getClassLoader().loadClass(className);
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public InputStream getPluginResourceAsStream(final String pluginKey, final String resourcePath)
    {
        final Plugin plugin = getEnabledPlugin(pluginKey);
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
     * @param plugin     the plugin to be replaced
     * @param descriptor the descriptor which caused the problem
     * @param throwable  the problem caught when enabling the descriptor
     * @return the UnloadablePlugin which replaced the broken plugin
     */
    private UnloadablePlugin replacePluginWithUnloadablePlugin(final Plugin plugin, final ModuleDescriptor<?> descriptor, final Throwable throwable)
    {
        final UnloadableModuleDescriptor unloadableDescriptor = UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin,
            descriptor, throwable);
        final UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin, unloadableDescriptor);

        unloadablePlugin.setUninstallable(plugin.isUninstallable());
        unloadablePlugin.setDeletable(plugin.isDeleteable());
        plugins.put(plugin.getKey(), unloadablePlugin);

        // Disable it
        disablePluginState(plugin, getStore());
        return unloadablePlugin;
    }

    public boolean isSystemPlugin(final String key)
    {
        final Plugin plugin = getPlugin(key);
        return (plugin != null) && plugin.isSystemPlugin();
    }

    public PluginRestartState getPluginRestartState(String key)
    {
        return getState().getPluginRestartState(key);
    }

    /**
     * @deprecated Since 2.0.0.beta2
     */
    @Deprecated
    public void setDescriptorParserFactory(final DescriptorParserFactory descriptorParserFactory)
    {}

    private static class PluginEnabledCondition implements WaitUntil.WaitCondition
    {
        private final Plugin plugin;

        public PluginEnabledCondition(final Plugin plugin)
        {
            this.plugin = plugin;
        }

        public boolean isFinished()
        {
            return plugin.getPluginState() == PluginState.ENABLED;
        }

        public String getWaitMessage()
        {
            return "Waiting until plugin " + plugin + " is enabled";
        }
    }

}
