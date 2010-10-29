package com.atlassian.plugin.manager;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;

public class TestEnabledModuleCachingPluginAccessor extends TestDefaultPluginManager
{
    private PluginAccessor delegate;
    private PluginAccessor cachingPluginAccessor;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        delegate = Mockito.mock(PluginAccessor.class);
        cachingPluginAccessor = new EnabledModuleCachingPluginAccessor(manager, pluginEventManager);
    }

    @Override
    protected PluginAccessor getPluginAccessor()
    {
        return cachingPluginAccessor;
    }

    public void testDelegateShouldCalculateAtMostOnce()
    {
        // call the cached method multiple times.
        cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
        cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        // broadcast new module and then call again.
        DummyParentDescriptor descriptor = mock(DummyParentDescriptor.class);
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(descriptor));
        cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        // should have been called at most once.
        verify(delegate, atMost(1)).getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);
    }

    public void testChildAndParentClassBeingTrackedSeparately()
    {
        cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyChildDescriptor.class);
        cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class);

        pluginEventManager.broadcast(new PluginModuleEnabledEvent(mock(DummyChildDescriptor.class)));
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(mock(DummyParentDescriptor.class)));

        assertEquals(1, cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyChildDescriptor.class).size());
        assertEquals(2, cachingPluginAccessor.getEnabledModuleDescriptorsByClass(DummyParentDescriptor.class).size());
    }

    public abstract class DummyParentDescriptor implements ModuleDescriptor
    {
    }

    public abstract class DummyChildDescriptor extends DummyParentDescriptor
    {
    }
}