package com.atlassian.plugin.predicate;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import junit.framework.TestCase;

/**
 * Testing {@link ModuleDescriptorClassModulePredicate}
 */
public class TestModuleDescriptorClassModulePredicate extends TestCase
{
    private ModulePredicate modulePredicate;

    protected void setUp() throws Exception
    {
        modulePredicate = new ModuleDescriptorClassModulePredicate(ModuleDescriptorStubA.class);
    }

    protected void tearDown() throws Exception
    {
        modulePredicate = null;
    }

    public void testCannotCreateWithNullModuleDescritptorClassesArray()
    {
        try
        {
            new ModuleDescriptorClassModulePredicate((Class[]) null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e)
        {
            // noop
        }
    }

    public void testMatchesModuleWithModuleDescriptorClassExactlyMatchingClass()
    {
        assertTrue(modulePredicate.matches(new ModuleDescriptorStubA()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorClassExtendingButNotExactlyMatchingClass()
    {
        assertTrue(modulePredicate.matches(new ModuleDescriptorStubB()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorClassNotMatchingClass()
    {
        assertFalse(modulePredicate.matches(new MockUnusedModuleDescriptor()));
    }

    private static class ModuleDescriptorStubA extends AbstractModuleDescriptor
    {
        public Object getModule()
        {
            return null;
        }
    }

    private static class ModuleDescriptorStubB extends ModuleDescriptorStubA
    {
    }
}
