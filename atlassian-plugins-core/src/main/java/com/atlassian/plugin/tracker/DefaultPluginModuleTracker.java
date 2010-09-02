package com.atlassian.plugin.tracker;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.google.common.base.Function;

import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.unmodifiableIterable;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads
 *
 * @since 2.6.0
 */
public class DefaultPluginModuleTracker<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker<M, T>
{
    private final CopyOnWriteArraySet<T> moduleDescriptors;
    private final PluginEventManager pluginEventManager;
    private final Class<T> moduleDescriptorClass;
    private final Customizer<M, T> pluginModuleTrackerCustomizer;
    private final ModuleTransformer<M, T> moduleTransformer = new ModuleTransformer<M, T>();

    public DefaultPluginModuleTracker(PluginAccessor pluginAccessor, PluginEventManager pluginEventManager,
                                      Class<T> moduleDescriptorClass)
    {
        this(pluginAccessor, pluginEventManager, moduleDescriptorClass, new NoOpPluginModuleTrackerCustomizer<M, T>());
    }

    public DefaultPluginModuleTracker(PluginAccessor pluginAccessor, PluginEventManager pluginEventManager,
                                      Class<T> moduleDescriptorClass,
                                      Customizer<M, T> pluginModuleTrackerCustomizer)
    {
        this.pluginEventManager = pluginEventManager;
        this.moduleDescriptorClass = moduleDescriptorClass;
        this.pluginModuleTrackerCustomizer = pluginModuleTrackerCustomizer;
        this.moduleDescriptors = new CopyOnWriteArraySet<T>();
        pluginEventManager.register(this);
        for (T descriptor : pluginAccessor.getEnabledModuleDescriptorsByClass(moduleDescriptorClass))
        {
            addDescriptor(descriptor);
        }
    }

    @PluginEventListener
    public void onPluginModuleEnabled(PluginModuleEnabledEvent event)
    {
        ModuleDescriptor<?> moduleDescriptor = event.getModule();
        if (moduleDescriptorClass.isAssignableFrom(moduleDescriptor.getClass()))
        {
            addDescriptor((T) moduleDescriptor);
        }
    }

    private void addDescriptor(T moduleDescriptor)
    {
        T descriptor = pluginModuleTrackerCustomizer.adding(moduleDescriptor);
        if (descriptor != null)
        {
            moduleDescriptors.add(descriptor);
        }
    }

    @PluginEventListener
    public void onPluginModuleDisabled(PluginModuleDisabledEvent event)
    {
        ModuleDescriptor moduleDescriptor = event.getModule();
        if (moduleDescriptorClass.isAssignableFrom(moduleDescriptor.getClass()))
        {
            removeDescriptor((T) moduleDescriptor);
        }

    }

    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        for (ModuleDescriptor<?> descriptor : event.getPlugin().getModuleDescriptors())
        {
            if (moduleDescriptorClass.isAssignableFrom(descriptor.getClass()))
            {
                removeDescriptor((T) descriptor);
            }
        }
    }

    private void removeDescriptor(T descriptor)
    {
        if (moduleDescriptors.remove(descriptor))
        {
            pluginModuleTrackerCustomizer.removed(descriptor);
        }
    }

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

    private static class NoOpPluginModuleTrackerCustomizer<M, T extends ModuleDescriptor<M>> implements PluginModuleTracker.Customizer<M, T>
    {
        public T adding(T descriptor)
        {
            return descriptor;
        }

        public void removed(T descriptor)
        {
        }
    }

    private static class ModuleTransformer<M, T extends ModuleDescriptor<M>> implements Function<T, M>
    {
        public M apply(final T from)
        {
            return from.getModule();
        }
    }

}
