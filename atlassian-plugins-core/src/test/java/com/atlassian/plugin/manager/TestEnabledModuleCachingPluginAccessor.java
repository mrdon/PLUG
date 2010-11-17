package com.atlassian.plugin.manager;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestEnabledModuleCachingPluginAccessor extends TestDefaultPluginManager
{
    private PluginAccessor delegate;
    private PluginAccessor cachingPluginAccessorToMock;
    private PluginAccessor cachingPluginAccessorToReal;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        delegate = Mockito.mock(PluginAccessor.class);
        cachingPluginAccessorToMock = new EnabledModuleCachingPluginAccessor(delegate, pluginEventManager);
        cachingPluginAccessorToReal = new EnabledModuleCachingPluginAccessor(manager, pluginEventManager);
    }

    @Override
    protected PluginAccessor getPluginAccessor()
    {
        return cachingPluginAccessorToReal;
    }

    public void testDelegateShouldCalculateAtMostOnce()
    {
        // call the cached method multiple times.
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        // broadcast new module and then call again.
        DummyParentDescriptor descriptor = mock(DummyParentDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        // should have been called at most once.
        verify(delegate, times(1)).getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
    }

    public void testFlushCacheAfterAnyPluginDisable()
    {
        // call the cached method multiple times.
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        pluginEventManager.broadcast(new PluginDisabledEvent(mock(Plugin.class)));
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        verify(delegate, times(2)).getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
    }

    public void testChildAndParentClassBeingTrackedSeparately()
    {
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyChildDescriptor.class);
        cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        pluginEventManager.broadcast(new PluginModuleEnabledEvent(mock(DummyChildDescriptor.class)));
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(mock(DummyParentDescriptor.class)));

        assertEquals(1, cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyChildDescriptor.class).size());
        assertEquals(2, cachingPluginAccessorToMock.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class).size());
    }

    public abstract class DummyParentDescriptor implements ModuleDescriptor
    {
    }

    public abstract class DummyChildDescriptor extends DummyParentDescriptor
    {
    }
}