package com.atlassian.plugin.manager;

import static com.google.common.collect.ImmutableList.copyOf;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.tracker.DefaultPluginModuleTracker;
import com.atlassian.plugin.tracker.PluginModuleTracker;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * A caching decorator which caches {@link #getEnabledModuleDescriptorsByClass(Class)} on {@link com.atlassian.plugin.PluginAccessor} interface.
 *
 * @since 2.7.0
 */
public final class EnabledModuleCachingPluginAccessor extends ForwardingPluginAccessor implements PluginAccessor
{
    private final PluginEventManager pluginEventManager;
    private final ConcurrentMap<Class<ModuleDescriptor<Object>>, PluginModuleTracker<Object, ModuleDescriptor<Object>>> cache = new MapMaker().makeComputingMap(new PluginModuleTrackerFactory());

    public EnabledModuleCachingPluginAccessor(final PluginAccessor delegate, final PluginEventManager pluginEventManager)
    {
        super(delegate);
        this.pluginEventManager = pluginEventManager;
    }

    @Override
    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz)
    {
        return copyOf(descriptors(descriptorClazz));
    }

    /**
     * Cache implementation.
     */
    <D> Iterable<D> descriptors(final Class<D> moduleDescriptorClass)
    {
        @SuppressWarnings("unchecked")
        final Iterable<D> descriptors = (Iterable<D>) cache.get(moduleDescriptorClass).getModuleDescriptors();
        return descriptors;
    }

    final class PluginModuleTrackerFactory implements Function<Class<ModuleDescriptor<Object>>, PluginModuleTracker<Object, ModuleDescriptor<Object>>>
    {
        public PluginModuleTracker<Object, ModuleDescriptor<Object>> apply(final Class<ModuleDescriptor<Object>> moduleDescriptorClass)
        {
            // need some generic trickery here as we don't know the specific moduleDescriptor type
            return DefaultPluginModuleTracker.create(delegate, pluginEventManager, moduleDescriptorClass);
        }
    }
}
