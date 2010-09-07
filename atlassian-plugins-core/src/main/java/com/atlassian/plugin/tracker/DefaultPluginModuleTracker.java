package com.atlassian.plugin.tracker;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.unmodifiableIterable;
import static java.util.Collections.singleton;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;

import com.google.common.base.Function;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads
 *
 * @since 2.6.0
 */
public class DefaultPluginModuleTracker<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker<M, T>
{
    private final PluginEventManager pluginEventManager;
    private final Class<T> moduleDescriptorClass;
    private final Customizer<M, T> pluginModuleTrackerCustomizer;
    private final CopyOnWriteArraySet<T> moduleDescriptors = new CopyOnWriteArraySet<T>();
    private final ModuleTransformer<M, T> moduleTransformer = new ModuleTransformer<M, T>();

    //
    // ctors
    //

    public DefaultPluginModuleTracker(final PluginAccessor pluginAccessor, final PluginEventManager pluginEventManager, final Class<T> moduleDescriptorClass)
    {
        this(pluginAccessor, pluginEventManager, moduleDescriptorClass, new NoOpPluginModuleTrackerCustomizer<M, T>());
    }

    public DefaultPluginModuleTracker(final PluginAccessor pluginAccessor, final PluginEventManager pluginEventManager, final Class<T> moduleDescriptorClass, final Customizer<M, T> pluginModuleTrackerCustomizer)
    {
        this.pluginEventManager = pluginEventManager;
        this.moduleDescriptorClass = moduleDescriptorClass;
        this.pluginModuleTrackerCustomizer = pluginModuleTrackerCustomizer;
        pluginEventManager.register(this);
        addDescriptors(pluginAccessor.getEnabledModuleDescriptorsByClass(moduleDescriptorClass));
    }

    //
    // PluginModuleTracker impl
    //

    public Iterable<T> getModuleDescriptors()
    {
        return unmodifiableIterable(moduleDescriptors);
    }

    public Iterable<M> getModules()
    {
        return transform(getModuleDescriptors(), moduleTransformer);
    }

    public int size()
    {
        return moduleDescriptors.size();
    }

    public void close()
    {
        pluginEventManager.unregister(this);
    }

    //
    // plugin event listening
    //

    @PluginEventListener
    public void onPluginModuleEnabled(final PluginModuleEnabledEvent event)
    {
        addDescriptors(singleton((ModuleDescriptor<?>) event.getModule()));
    }

    @PluginEventListener
    public void onPluginModuleDisabled(final PluginModuleDisabledEvent event)
    {
        removeDescriptors(singleton((ModuleDescriptor<?>) event.getModule()));
    }

    @PluginEventListener
    public void onPluginDisabled(final PluginDisabledEvent event)
    {
        removeDescriptors(event.getPlugin().getModuleDescriptors());
    }

    //
    // module descriptor management
    //

    private void addDescriptors(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        for (final T descriptor : filtered(descriptors))
        {
            final T customized = pluginModuleTrackerCustomizer.adding(descriptor);
            if (customized != null)
            {
                moduleDescriptors.add(customized);
            }
        }
    }

    private void removeDescriptors(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        for (final T descriptor : filtered(descriptors))
        {
            if (moduleDescriptors.remove(descriptor))
            {
                pluginModuleTrackerCustomizer.removed(descriptor);
            }
        }
    }

    /**
     * The descriptors that match the supplied class.
     */
    private Iterable<T> filtered(final Iterable<? extends ModuleDescriptor<?>> descriptors)
    {
        return filter(descriptors, moduleDescriptorClass);
    }

    //
    // inner classes
    //

    private static class NoOpPluginModuleTrackerCustomizer<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker.Customizer<M, T>
    {
        public T adding(final T descriptor)
        {
            return descriptor;
        }

        public void removed(final T descriptor)
        {}
    }

    /**
     * Safely get the Module from a {@link ModuleDescriptor}.
     */
    private static class ModuleTransformer<M, T extends ModuleDescriptor<M>> implements Function<T, M>
    {
        public M apply(final T from)
        {
            return from.getModule();
        }
    }
}
