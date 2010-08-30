package com.atlassian.plugin.tracker;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.util.ReadOnlyIterator;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks enabled plugin module descriptors, focusing on fast reads
 *
 * @since 2.6.0
 */
public class DefaultPluginModuleTracker<T extends ModuleDescriptor> implements PluginModuleTracker<T>
{
    private final CopyOnWriteArraySet<T> moduleDescriptors;
    private final PluginEventManager pluginEventManager;
    private final Customizer pluginModuleTrackerCustomizer;

    public DefaultPluginModuleTracker(PluginAccessor pluginAccessor, PluginEventManager pluginEventManager)
    {
        this(pluginAccessor, pluginEventManager, new NoOpPluginModuleTrackerCustomizer());
    }
    public DefaultPluginModuleTracker(PluginAccessor pluginAccessor, PluginEventManager pluginEventManager,
                                      Customizer pluginModuleTrackerCustomizer)
    {
        this.pluginEventManager = pluginEventManager;
        this.pluginModuleTrackerCustomizer = pluginModuleTrackerCustomizer;
        this.moduleDescriptors = new CopyOnWriteArraySet<T>();
        pluginEventManager.register(this);
        for (Plugin plugin : pluginAccessor.getEnabledPlugins())
        {
            for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
            {
                if (pluginAccessor.isPluginModuleEnabled(descriptor.getCompleteKey()))
                {
                    addDescriptor(descriptor);
                }
            }
        }
    }

    @PluginEventListener
    public void onPluginModuleEnabled(PluginModuleEnabledEvent event)
    {
        ModuleDescriptor module = event.getModule();
        addDescriptor(module);
    }

    private void addDescriptor(ModuleDescriptor module)
    {
        ModuleDescriptor<T> descriptor = pluginModuleTrackerCustomizer.adding(module);
        if (descriptor != null)
        {
            moduleDescriptors.add((T) descriptor);
        }
    }

    @PluginEventListener
    public void onPluginModuleDisabled(PluginModuleDisabledEvent event)
    {
        ModuleDescriptor descriptor = event.getModule();
        removeDescriptor(descriptor);
    }

    @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        for (ModuleDescriptor descriptor : event.getPlugin().getModuleDescriptors())
        {
            removeDescriptor(descriptor);
        }
    }

    private void removeDescriptor(ModuleDescriptor descriptor)
    {
        if (moduleDescriptors.remove(descriptor))
        {
            pluginModuleTrackerCustomizer.removed(descriptor);
        }
    }

    public Iterable<T> getModuleDescriptors()
    {
        return new ReadOnlyIterator<T>(moduleDescriptors.iterator());
    }

    public <MT> Iterable<MT> getModules(Class<MT> moduleClass)
    {
        return new ModuleIterator<MT>((Iterator<ModuleDescriptor<MT>>) moduleDescriptors.iterator());
    }

    public int size()
    {
        return moduleDescriptors.size();
    }

    public void close()
    {
        pluginEventManager.unregister(this);
    }

    private static class ModuleIterator<T> implements Iterator<T>, Iterable<T>
    {
        private final Iterator<ModuleDescriptor<T>> descriptors;

        public ModuleIterator(Iterator<ModuleDescriptor<T>> descriptors)
        {
            this.descriptors = descriptors;
        }

        public boolean hasNext()
        {
            return descriptors.hasNext();
        }

        public T next()
        {
            ModuleDescriptor<T> descriptor = descriptors.next();
            return descriptor.getModule();
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Remove not supported");
        }

        public Iterator<T> iterator()
        {
            return this;
        }
    }

    private static class NoOpPluginModuleTrackerCustomizer implements PluginModuleTracker.Customizer
    {
        public ModuleDescriptor adding(ModuleDescriptor descriptor)
        {
            return descriptor;
        }

        public void removed(ModuleDescriptor descriptor)
        {
        }
    }

}
