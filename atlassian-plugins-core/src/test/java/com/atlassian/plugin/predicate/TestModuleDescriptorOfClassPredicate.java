package com.atlassian.plugin.predicate;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import junit.framework.TestCase;

/**
 * Testing {@link ModuleDescriptorOfClassPredicate}
 */
public class TestModuleDescriptorOfClassPredicate extends TestCase
{
    private ModuleDescriptorPredicate moduleDescriptorPredicate;

    protected void setUp() throws Exception
    {
        moduleDescriptorPredicate = new ModuleDescriptorOfClassPredicate(ModuleDescriptorStubA.class);
    }

    protected void tearDown() throws Exception
    {
        moduleDescriptorPredicate = null;
    }

    public void testCannotCreateWithNullModuleDescritptorClassesArray()
    {
        try
        {
            new ModuleDescriptorOfClassPredicate((Class[]) null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e)
        {
            // noop
        }
    }

    public void testMatchesModuleWithModuleDescriptorClassExactlyMatchingClass()
    {
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubA()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorClassExtendingButNotExactlyMatchingClass()
    {
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubB()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorClassNotMatchingClass()
    {
        assertFalse(moduleDescriptorPredicate.matches(new MockUnusedModuleDescriptor()));
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
