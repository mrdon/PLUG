package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import org.slf4j.Logger;

/**
 */
public class TestPrefixDelegatingModuleFactory extends TestCase
{
    PrefixDelegatingModuleFactory prefixDelegatingModuleFactory;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCreateBean() throws Exception
    {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");
        Object bean = new Object();

        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenReturn(bean);
        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));

        final Object returnedBean = this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    public void testCreateBeanWithDynamicModuleFactory() throws Exception
    {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");

        Object bean = new Object();
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        ContainerAccessor containerAccessor = mock(ContainerAccessor.class);
        ContainerManagedPlugin plugin = mock(ContainerManagedPlugin.class);
        when(plugin.getContainerAccessor()).thenReturn(containerAccessor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(containerAccessor.getBeansOfType(PrefixModuleFactory.class)).thenReturn(Collections.singleton(moduleFactory));

        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenReturn(bean);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.<PrefixModuleFactory>emptySet());

        final Object returnedBean = this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    public void testCreateBeanThrowsNoClassDefFoundError() throws Exception
    {
        _testCreateWithThrowableCausingErrorLogMessage(new NoClassDefFoundError());
    }

    public void testCreateBeanThrowsUnsatisfiedDependencyException() throws Exception
    {
        _testCreateWithThrowableCausingErrorLogMessage(new UnsatisfiedDependencyException());
    }

    public void testCreateBeanThrowsLinkageError() throws Exception
    {
        _testCreateWithThrowableCausingErrorLogMessage(new LinkageError());
    }

    private void _testCreateWithThrowableCausingErrorLogMessage(Throwable throwable)
    {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");
        Logger log = mock(Logger.class);


        Plugin plugin = mock(Plugin.class);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenThrow(throwable);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));
        this.prefixDelegatingModuleFactory.log = log;

        try
        {
            this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);

            fail("Should not return");
        }
        catch (Throwable err)
        {
            verify(log).error(anyString());
        }
    }

    public void testCreateBeanFailed() throws Exception
    {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("bob");
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));

        try
        {
            this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);

            fail("Should not return, there is no module prefix provider for jira");
        }
        catch (PluginParseException ex)
        {
            //Ex
            assertEquals("Failed to create a module. Prefix 'jira' not supported", ex.getMessage());
        }
        verify(moduleFactory, never()).createModule("doSomething", moduleDescriptor);
    }

    private static class UnsatisfiedDependencyException extends RuntimeException
    {}
}
