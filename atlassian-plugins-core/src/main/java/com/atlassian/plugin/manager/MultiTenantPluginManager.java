package com.atlassian.plugin.manager;

import com.atlassian.multitenant.MultiTenantAwareComponentMap;
import com.atlassian.plugin.ModuleCompleteKey;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginRestartState;
import com.atlassian.plugin.PluginSystemLifecycle;
import com.atlassian.plugin.event.NotificationException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.MultiTenantPluginDisabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginEnabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginModuleEnabledEvent;
import com.atlassian.plugin.predicate.CompositeModuleDescriptorPredicate;
import com.atlassian.plugin.predicate.EnabledModulePredicate;
import com.atlassian.plugin.predicate.EnabledPluginPredicate;
import com.atlassian.plugin.predicate.ModuleDescriptorOfClassPredicate;
import com.atlassian.plugin.predicate.ModuleDescriptorOfTypePredicate;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;
import com.atlassian.plugin.predicate.ModuleOfClassPredicate;
import com.atlassian.plugin.predicate.PluginPredicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Mostly this just delegates to the actual plugin manager, however, when plugins are enabled/disabled, it only
 * delegates to the actual plugin manager iff it's the last tenant to disable or the first tenant to enable.  It also
 * filters all calls to getEnabled*.
 * <p/>
 * The plugin manager passed in should be configured to use a non database dependent state store (either a memory state
 * state store or some XML multitenant system wide based one), while this one should be configured to use a tenant
 * specific one.
 * <p/>
 * Anything that caches the results of getEnabled* must do so in a multi tenant way.  Refreshing those caches can be
 * done by listening for MultiTenantPlugin[Module](En/Dis)abledEvent.
 *
 * @since v2.6
 */
public class MultiTenantPluginManager implements PluginController, PluginAccessor, PluginSystemLifecycle
{
    private static final Log log = LogFactory.getLog(MultiTenantPluginManager.class);

    private final PluginAccessor pluginAccessor;
    private final PluginController pluginController;
    private final PluginSystemLifecycle pluginSystemLifecycle;
    private final MultiTenantAwareComponentMap<PluginPersistentStateStore> storeMap;
    private final ModuleDescriptorFactory moduleDescriptorFactory;
    private final PluginEventManager pluginEventManager;

    public MultiTenantPluginManager(DefaultPluginManager pluginManager,
            MultiTenantAwareComponentMap<PluginPersistentStateStore> storeMap,
            ModuleDescriptorFactory moduleDescriptorFactory, PluginEventManager pluginEventManager)
    {
        this.pluginAccessor = pluginManager;
        this.pluginController = pluginManager;
        this.pluginSystemLifecycle = pluginManager;
        this.storeMap = storeMap;
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        this.pluginEventManager = pluginEventManager;
    }

    // Overridden methods

    public void enablePlugin(String key)
    {
        // Check if its enabled
        if (pluginAccessor.isPluginEnabled(key))
        {
            // Remove the state from the store
            PluginPersistentStateStore store = storeMap.get();
            store.save(new PluginPersistentState.Builder(store.load()).removeState(key).toState());
            pluginEventManager.broadcast(new MultiTenantPluginEnabledEvent(pluginAccessor.getPlugin(key)));
        }
        else
        {
            // Currently don't support enabling plugins that are disabled by the system
            throw new UnsupportedOperationException("Can not enable plugins that are disabled by the system: " + key);
        }
    }

    public void disablePlugin(String key)
    {
        Plugin plugin = pluginAccessor.getPlugin(key);
        if (plugin != null)
        {
            PluginPersistentStateStore store = storeMap.get();
            store.save(new PluginPersistentState.Builder(store.load()).setEnabled(plugin, false).toState());
            pluginEventManager.broadcast(new MultiTenantPluginDisabledEvent(plugin));
        }
    }

    public void enablePluginModule(String completeKey)
    {
        if (pluginAccessor.isPluginModuleEnabled(completeKey))
        {
            PluginPersistentStateStore store = storeMap.get();
            store.save(new PluginPersistentState.Builder(store.load()).removeState(completeKey).toState());
            pluginEventManager.broadcast(new MultiTenantPluginModuleEnabledEvent(
                    pluginAccessor.getPluginModule(completeKey)));
        }
        else
        {
            // Currently don't support enabling plugins that are disabled by the system
            throw new UnsupportedOperationException("Can not enable plugins that are disabled by the system: " +
                    completeKey);
        }
    }

    public void disablePluginModule(String completeKey)
    {
        ModuleDescriptor<?> module = pluginAccessor.getPluginModule(completeKey);
        if (module != null)
        {
            PluginPersistentStateStore store = storeMap.get();
            store.save(new PluginPersistentState.Builder(store.load()).setEnabled(module, false).toState());
            pluginEventManager.broadcast(new MultiTenantPluginModuleDisabledEvent(module));
        }
    }

    public Collection<Plugin> getEnabledPlugins()
    {
        // The EnabledPluginPredecate will delegate to this plugin accessor, not the underlying one.
        return pluginAccessor.getPlugins(new EnabledPluginPredicate(this));
    }

    public Plugin getEnabledPlugin(String pluginKey) throws IllegalArgumentException
    {
        if (!isPluginEnabled(pluginKey))
        {
            return null;
        }
        return pluginAccessor.getPlugin(pluginKey);
    }

    public ModuleDescriptor<?> getEnabledPluginModule(String completeKey)
    {
        final ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        // If it's disabled, return null
        if (!isPluginModuleEnabled(completeKey))
        {
            return null;
        }

        return pluginAccessor.getPluginModule(completeKey);
    }

    public boolean isPluginEnabled(String key) throws IllegalArgumentException
    {
        // Plugin is enabled if the underlying plugin system thinks it is and if there is either no entry
        // in the map, or if the entry in the map is true
        if (pluginAccessor.isPluginEnabled(key))
        {
            final Boolean enabledForTenant = storeMap.get().load().getMap().get(key);
            return enabledForTenant == null || enabledForTenant;
        }
        else
        {
            return false;
        }
    }

    public boolean isPluginModuleEnabled(String completeKey)
    {
        if (pluginAccessor.isPluginModuleEnabled(completeKey))
        {
            final Boolean enabledForTenant = storeMap.get().load().getMap().get(completeKey);
            return enabledForTenant == null || enabledForTenant;
        }
        else
        {
            return false;
        }
    }

    public <M> List<M> getEnabledModulesByClass(Class<M> moduleClass)
    {
        return new ArrayList<M>(pluginAccessor.getModules(new CompositeModuleDescriptorPredicate<M>(
                new ModuleOfClassPredicate<M>(moduleClass),
                new EnabledModulePredicate(this))));
    }

    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(Class<ModuleDescriptor<M>>[] descriptorClazz, Class<M> moduleClass)
    {
        return new ArrayList<M>(pluginAccessor.getModules(new CompositeModuleDescriptorPredicate<M>(
                new ModuleOfClassPredicate<M>(moduleClass),
                new ModuleDescriptorOfClassPredicate<M>(descriptorClazz),
                new EnabledModulePredicate(this))));
    }

    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(Class<ModuleDescriptor<M>> descriptorClass, Class<M> moduleClass)
    {
        // Generic arrays SUCK
        Class<ModuleDescriptor<M>>[] classes = (Class<ModuleDescriptor<M>>[]) new Class[1];
        classes[0] = descriptorClass;
        return getEnabledModulesByClassAndDescriptor(classes, moduleClass);
    }

    /**
     * This method has been reverted to pre PLUG-40 to fix performance issues that were encountered during load testing.
     * This should be reverted to the state it was in at 54639 when the fundamental issue leading to this slowdown has
     * been corrected (that is, slowness of PluginClassLoader).
     *
     * @see PluginAccessor#getEnabledModuleDescriptorsByClass(Class)
     */
    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz)
    {
        final List<D> result = new LinkedList<D>();
        for (final Plugin plugin : getEnabledPlugins())
        {
            for (final ModuleDescriptor<?> module : plugin.getModuleDescriptors())
            {
                if (descriptorClazz.isInstance(module))
                {
                    if (isPluginModuleEnabled(module.getCompleteKey()))
                    {
                        @SuppressWarnings ("unchecked")
                        final D moduleDescriptor = (D) module;
                        result.add(moduleDescriptor);
                    }
                }
            }
        }

        return result;
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz, boolean verbose)
    {
        return getEnabledModuleDescriptorsByClass(descriptorClazz);
    }

    @Deprecated
    public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(String type) throws PluginParseException
    {
        return new ArrayList<ModuleDescriptor<M>>(pluginAccessor.getModuleDescriptors(new CompositeModuleDescriptorPredicate<M>(
                new ModuleDescriptorOfTypePredicate<M>(moduleDescriptorFactory, type),
                new EnabledModulePredicate(this))));
    }

    // Unsupported operations

    public String installPlugin(PluginArtifact pluginArtifact) throws PluginParseException
    {
        throw new UnsupportedOperationException("Multi-Tenant plugin manager does not support dynamic installation of plugins");
    }

    public Set<String> installPlugins(PluginArtifact... pluginArtifacts) throws PluginParseException
    {
        throw new UnsupportedOperationException("Multi-Tenant plugin manager does not support dynamic installation of plugins");
    }

    public void uninstall(Plugin plugin) throws PluginException
    {
        throw new UnsupportedOperationException("Multi-Tenant plugin manager does not support dynamic installation of plugins");
    }


    // Delegate methods

    public int scanForNewPlugins() throws PluginParseException
    {
        return pluginController.scanForNewPlugins();
    }

    public void init() throws PluginParseException, NotificationException
    {
        pluginSystemLifecycle.init();
    }

    public void shutdown()
    {
        pluginSystemLifecycle.shutdown();
    }

    public void warmRestart()
    {
        pluginSystemLifecycle.warmRestart();
    }

    public void disablePluginWithoutPersisting(String key)
    {
        pluginController.disablePluginWithoutPersisting(key);
    }

    public Collection<Plugin> getPlugins()
    {
        return pluginAccessor.getPlugins();
    }

    public Collection<Plugin> getPlugins(PluginPredicate pluginPredicate)
    {
        return pluginAccessor.getPlugins(pluginPredicate);
    }

    public <M> Collection<M> getModules(ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        return pluginAccessor.getModules(moduleDescriptorPredicate);
    }

    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        return pluginAccessor.getModuleDescriptors(moduleDescriptorPredicate);
    }

    public Plugin getPlugin(String key) throws IllegalArgumentException
    {
        return pluginAccessor.getPlugin(key);
    }

    public ModuleDescriptor<?> getPluginModule(String completeKey)
    {
        return pluginAccessor.getPluginModule(completeKey);
    }


    public InputStream getDynamicResourceAsStream(String resourcePath)
    {
        return pluginAccessor.getDynamicResourceAsStream(resourcePath);
    }

    @Deprecated
    public InputStream getPluginResourceAsStream(String pluginKey, String resourcePath)
    {
        return pluginAccessor.getPluginResourceAsStream(pluginKey, resourcePath);
    }

    @Deprecated
    public Class<?> getDynamicPluginClass(String className) throws ClassNotFoundException
    {
        return pluginAccessor.getDynamicPluginClass(className);
    }

    public ClassLoader getClassLoader()
    {
        return pluginAccessor.getClassLoader();
    }

    public boolean isSystemPlugin(String key)
    {
        return pluginAccessor.isSystemPlugin(key);
    }

    public PluginRestartState getPluginRestartState(String key)
    {
        return pluginAccessor.getPluginRestartState(key);
    }
}