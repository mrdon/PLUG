package com.atlassian.plugin.osgi.factory;

import java.util.Collections;
import java.util.List;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.StubServletModuleDescriptor;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

import org.osgi.framework.Bundle;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUnrecognizedModuleDescriptorServiceTrackerCustomizer extends TestCase
{
    private OsgiPlugin plugin;
    private UnrecognizedModuleDescriptorServiceTrackerCustomizer instance;

    protected void setUp() throws Exception
    {
        super.setUp();
        plugin = mock(OsgiPlugin.class);
        Bundle bundle = mock(Bundle.class);
        when(plugin.getBundle()).thenReturn(bundle);
        instance = new UnrecognizedModuleDescriptorServiceTrackerCustomizer(plugin, null);
    }

    public void testGetModuleDescriptorsByDescriptorClass()
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        StubServletModuleDescriptor stubServletModuleDescriptor =
                new StubServletModuleDescriptor(moduleFactory, servletModuleManager);
        when(plugin.getModuleDescriptors()).thenReturn(
                Collections.<ModuleDescriptor<?>>singleton(stubServletModuleDescriptor));

        List<ServletModuleDescriptor> result = instance.getModuleDescriptorsByDescriptorClass(
                ServletModuleDescriptor.class);
        assertEquals(Collections.<ServletModuleDescriptor>singletonList(stubServletModuleDescriptor), result);
    }

    public void testGetModuleDescriptorsByDescriptorClassWithSubclass()
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        ServletModuleDescriptor servletModuleDescriptor =
                new ServletModuleDescriptor(moduleFactory, servletModuleManager);
        when(plugin.getModuleDescriptors()).thenReturn(
                Collections.<ModuleDescriptor<?>>singleton(servletModuleDescriptor));

        List<StubServletModuleDescriptor> result = instance.getModuleDescriptorsByDescriptorClass(
                StubServletModuleDescriptor.class);
        assertTrue(result.isEmpty());
    }
}
