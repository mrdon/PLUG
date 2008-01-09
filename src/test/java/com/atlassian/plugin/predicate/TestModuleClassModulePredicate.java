package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

/**
 */
public class TestModuleClassModulePredicate extends TestCase
{
    private final static Class TEST_MODULE_CLASS = TestCase.class;

    private ModulePredicate modulePredicate;

    private Mock mockModuleDescriptor;
    private ModuleDescriptor moduleDescriptor;

    protected void setUp() throws Exception
    {
        modulePredicate = new ModuleClassModulePredicate(TEST_MODULE_CLASS);

        mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
    }

    protected void tearDown() throws Exception
    {
        modulePredicate = null;
        moduleDescriptor = null;
        mockModuleDescriptor = null;
    }

    public void testCannotCreateWithNullClass()
    {
        try
        {
            new ModuleClassModulePredicate(null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e)
        {
            // noop
        }
    }

    public void testMatchesModuleExtendingClass()
    {
        mockModuleDescriptor.matchAndReturn("getModuleClass", this.getClass());
        assertTrue(modulePredicate.matches(moduleDescriptor));
    }

    public void testDoesNotMatchModuleNotExtendingClass()
    {
        mockModuleDescriptor.matchAndReturn("getModuleClass", Object.class);
        assertFalse(modulePredicate.matches(moduleDescriptor));
    }
}
