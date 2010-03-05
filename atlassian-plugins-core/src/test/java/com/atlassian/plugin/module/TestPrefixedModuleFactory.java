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
public class TestPrefixedModuleFactory extends TestCase
{
    PrefixedModuleFactory prefixedModuleFactory;

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
        ModuleFactory mockModuleFactory = mock(ModuleFactory.class);
        Object bean = new Object();

        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(mockModuleFactory.createModule("doSomething", moduleDescriptor)).thenReturn(bean);
        this.prefixedModuleFactory = new PrefixedModuleFactory(Collections.singletonMap("jira", mockModuleFactory));

        final Object returnedBean = this.prefixedModuleFactory.createModule("jira:doSomething", moduleDescriptor);
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
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        Logger log = mock(Logger.class);


        Plugin plugin = mock(Plugin.class);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenThrow(throwable);

        this.prefixedModuleFactory = new PrefixedModuleFactory(Collections.singletonMap("jira", moduleFactory));
        this.prefixedModuleFactory.log = log;

        try
        {
            this.prefixedModuleFactory.createModule("jira:doSomething", moduleDescriptor);

            fail("Should not return");
        }
        catch (Throwable err)
        {
            verify(log).error(anyString());
        }
    }

    public void testCreateBeanFailed() throws Exception
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);

        this.prefixedModuleFactory = new PrefixedModuleFactory(Collections.singletonMap("foo", moduleFactory));

        try
        {
            this.prefixedModuleFactory.createModule("jira:doSomething", moduleDescriptor);

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
