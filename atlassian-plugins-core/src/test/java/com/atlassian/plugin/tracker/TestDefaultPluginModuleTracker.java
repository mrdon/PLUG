package com.atlassian.plugin.tracker;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestDefaultPluginModuleTracker extends TestCase
{
    private PluginAccessor pluginAccessor;
    private PluginEventManager pluginEventManager;
    private DefaultPluginModuleTracker tracker;

    @Override
    protected void setUp() throws Exception
    {
        pluginAccessor = mock(PluginAccessor.class);
        when(pluginAccessor.getEnabledPlugins()).thenReturn(Collections.<Plugin>emptyList());
        pluginEventManager = new DefaultPluginEventManager();
        tracker = new DefaultPluginModuleTracker(pluginAccessor, pluginEventManager);
    }

    public void testAddModule()
    {
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        assertEquals(descriptor, tracker.getModuleDescriptors().iterator().next());
    }

    public void testAddModuleThenRemove()
    {
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        assertEquals(descriptor, tracker.getModuleDescriptors().iterator().next());
        pluginEventManager.broadcast(new PluginModuleDisabledEvent(descriptor));
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
    }

    public void testRemovePlugin()
    {
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        Plugin plugin = mock(Plugin.class);
        when(descriptor.getPlugin()).thenReturn(plugin);
        when(plugin.getModuleDescriptors()).thenReturn(Arrays.<ModuleDescriptor<?>>asList(descriptor));

        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        pluginEventManager.broadcast(new PluginDisabledEvent(plugin));
        assertEquals(0, tracker.size());
    }

    public void testAddModuleWithCustomizer()
    {
        ModuleDescriptor oldDescriptor = mock(ModuleDescriptor.class);
        final ModuleDescriptor newDescriptor = mock(ModuleDescriptor.class);
        PluginModuleTracker tracker = new DefaultPluginModuleTracker(pluginAccessor, pluginEventManager, new PluginModuleTracker.Customizer()
        {
            public ModuleDescriptor adding(ModuleDescriptor descriptor)
            {
                return newDescriptor;
            }

            public void removed(ModuleDescriptor descriptor)
            {
            }
        });
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(oldDescriptor));
        assertEquals(1, tracker.size());
        assertEquals(newDescriptor, tracker.getModuleDescriptors().iterator().next());
    }

    public void testRemoveModuleWithCustomizer()
    {
        ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        final AtomicBoolean removedCalled = new AtomicBoolean();
        PluginModuleTracker tracker = new DefaultPluginModuleTracker(pluginAccessor, pluginEventManager, new PluginModuleTracker.Customizer()
        {
            public ModuleDescriptor adding(ModuleDescriptor descriptor)
            {
                return descriptor;
            }

            public void removed(ModuleDescriptor descriptor)
            {
                removedCalled.set(true);
            }
        });
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        pluginEventManager.broadcast(new PluginModuleDisabledEvent(descriptor));
        assertTrue(removedCalled.get());
    }

}
