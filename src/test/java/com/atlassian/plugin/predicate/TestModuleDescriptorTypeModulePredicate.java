package com.atlassian.plugin.predicate;

import junit.framework.TestCase;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

/**
 * Testing {@link ModuleDescriptorTypeModulePredicate}
 */
public class TestModuleDescriptorTypeModulePredicate extends TestCase
{
    private ModulePredicate modulePredicate;

    public void testMatchesModuleWithModuleDescriptorMatchingType()
    {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubA.class);

        modulePredicate = new ModuleDescriptorTypeModulePredicate((ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy(), "test-module-type");
        assertTrue(modulePredicate.matches(new ModuleDescriptorStubB()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorNotMatchingType()
    {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubB.class);

        modulePredicate = new ModuleDescriptorTypeModulePredicate((ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy(), "test-module-type");
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
