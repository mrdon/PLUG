package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class TestDefaultModuleClassFactory extends TestCase
{
    ModuleClassFactory moduleCreator;

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
        ModuleCreator moduleCreator = mock(ModuleCreator.class);
        final List<ModuleCreator> creators = new ArrayList<ModuleCreator>();
        creators.add(moduleCreator);
        Object bean = new Object();

        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleCreator.getPrefix()).thenReturn("jira");
        when(moduleCreator.createBean("doSomething", moduleDescriptor)).thenReturn(bean);
        this.moduleCreator = new DefaultModuleClassFactory(creators);

        final Object returnedBean = this.moduleCreator.createModuleClass("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    public void testCreateBeanFailed() throws Exception
    {
        ModuleCreator moduleCreator = mock(ModuleCreator.class);
        final List<ModuleCreator> creators = new ArrayList<ModuleCreator>();
        creators.add(moduleCreator);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);

        when(moduleCreator.getPrefix()).thenReturn("bob");
        this.moduleCreator = new DefaultModuleClassFactory(creators);

        try
        {
            this.moduleCreator.createModuleClass("jira:doSomething", moduleDescriptor);

            fail("Should not return, there is no module prefix provider for jira");
        }
        catch (PluginParseException ex)
        {
            //Ex
            assertEquals("Failed to create a module class. Prefix 'jira' not supported", ex.getMessage());
        }
        verify(moduleCreator, times(2)).getPrefix();
        verify(moduleCreator, never()).createBean("doSomething", moduleDescriptor);
    }

}
