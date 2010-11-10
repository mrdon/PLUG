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
        tracker = new DefaultPluginModuleTracker<Object, MyModuleDescriptor>(pluginAccessor, pluginEventManager, MyModuleDescriptor.class);
    }

    public void testAddModule()
    {
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
        MyModuleDescriptor descriptor = mock(MyModuleDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        assertEquals(descriptor, tracker.getModuleDescriptors().iterator().next());
    }

    public void testAddModuleThenRemove()
    {
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
        MyModuleDescriptor descriptor = mock(MyModuleDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        assertEquals(descriptor, tracker.getModuleDescriptors().iterator().next());
        pluginEventManager.broadcast(new PluginModuleDisabledEvent(descriptor));
        assertFalse(tracker.getModuleDescriptors().iterator().hasNext());
        assertEquals(0, tracker.size());
    }

    public void testRemovePlugin()
    {
        MyModuleDescriptor descriptor = mock(MyModuleDescriptor.class);
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
        MyModuleDescriptor oldDescriptor = mock(MyModuleDescriptor.class);
        final MyModuleDescriptor newDescriptor = mock(MyModuleDescriptor.class);
        PluginModuleTracker tracker = new DefaultPluginModuleTracker<Object, MyModuleDescriptor>(
                pluginAccessor, pluginEventManager,
                MyModuleDescriptor.class,
                new PluginModuleTracker.Customizer<Object, MyModuleDescriptor>()
                {
                    public MyModuleDescriptor adding(MyModuleDescriptor descriptor)
                    {
                        return newDescriptor;
                    }

                    public void removed(MyModuleDescriptor descriptor)
                    {
                    }
                });
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(oldDescriptor));
        assertEquals(1, tracker.size());
        assertEquals(newDescriptor, tracker.getModuleDescriptors().iterator().next());
    }

    public void testRemoveModuleWithCustomizer()
    {
        MyModuleDescriptor descriptor = mock(MyModuleDescriptor.class);
        final AtomicBoolean removedCalled = new AtomicBoolean();
        PluginModuleTracker tracker = new DefaultPluginModuleTracker<Object, MyModuleDescriptor>(
                pluginAccessor, pluginEventManager,
                MyModuleDescriptor.class,
                new PluginModuleTracker.Customizer<Object, MyModuleDescriptor>()
                {
                    public MyModuleDescriptor adding(MyModuleDescriptor descriptor)
                    {
                        return descriptor;
                    }

                    public void removed(MyModuleDescriptor descriptor)
                    {
                        removedCalled.set(true);
                    }
                });
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        assertEquals(1, tracker.size());
        pluginEventManager.broadcast(new PluginModuleDisabledEvent(descriptor));
        assertTrue(removedCalled.get());
    }

    public interface MyModuleDescriptor extends ModuleDescriptor<Object>
    {
    }

}
