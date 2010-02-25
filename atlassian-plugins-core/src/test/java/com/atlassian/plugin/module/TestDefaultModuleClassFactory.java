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
    ModuleClassFactory moduleClassFactory;

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
        this.moduleClassFactory = new DefaultModuleClassFactory(creators);

        final Object returnedBean = this.moduleClassFactory.createModuleClass("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    public void testCreateBeanFailed() throws Exception
    {
        ModuleCreator moduleCreator = mock(ModuleCreator.class);
        final List<ModuleCreator> creators = new ArrayList<ModuleCreator>();
        creators.add(moduleCreator);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);

        when(moduleCreator.getPrefix()).thenReturn("bob");
        this.moduleClassFactory = new DefaultModuleClassFactory(creators);

        try
        {
            this.moduleClassFactory.createModuleClass("jira:doSomething", moduleDescriptor);

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

    public void testModuleCreatorWithSamePrefix() throws Exception
    {
        ModuleCreator moduleCreator1 = mock(ModuleCreator.class);
        ModuleCreator moduleCreator2 = mock(ModuleCreator.class);

        List<ModuleCreator> moduleCreators = new ArrayList<ModuleCreator>();
        moduleCreators.add(moduleCreator1);
        moduleCreators.add(moduleCreator2);

        when(moduleCreator1.getPrefix()).thenReturn("blah");
        when(moduleCreator2.getPrefix()).thenReturn("blah");

        try
        {
            moduleClassFactory = new DefaultModuleClassFactory(moduleCreators);
            fail("DefaultModuleClassFactory should not allow register same prefix twice");
        }
        catch (IllegalArgumentException ex)
        {
            assertEquals("Module Creator with the prefix 'blah' is already registered.", ex.getMessage());
        }
    }

    public void testModuleCreatorWithNullPrefix() throws Exception
    {
        ModuleCreator moduleCreator1 = mock(ModuleCreator.class);

        List<ModuleCreator> moduleCreators = new ArrayList<ModuleCreator>();
        moduleCreators.add(moduleCreator1);

        when(moduleCreator1.getPrefix()).thenReturn(null);

        try
        {
            moduleClassFactory = new DefaultModuleClassFactory(moduleCreators);
            fail("DefaultModuleClassFactory should not allow register same prefix twice");
        }
        catch (IllegalArgumentException ex)
        {
            assertEquals("Module Creator cannot have a NULL prefix", ex.getMessage());
        }
    }

}
