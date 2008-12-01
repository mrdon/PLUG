package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import junit.framework.TestCase;

/**
 * Testing {@link ModuleDescriptorOfTypePredicate}
 */
public class TestModuleDescriptorOfTypePredicate extends TestCase
{
    public void testMatchesModuleWithModuleDescriptorMatchingType()
    {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubA.class);

        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<Object> moduleDescriptorPredicate = new ModuleDescriptorOfTypePredicate<Object, Object>(
            (ModuleDescriptorFactory<Object, ModuleDescriptor<? extends Object>>) mockModuleDescriptorFactory.proxy(), "test-module-type");
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubB()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorNotMatchingType()
    {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubB.class);

        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<Object> moduleDescriptorPredicate = new ModuleDescriptorOfTypePredicate<Object, Object>(
            (ModuleDescriptorFactory<Object, ModuleDescriptor<? extends Object>>) mockModuleDescriptorFactory.proxy(), "test-module-type");
        assertFalse(moduleDescriptorPredicate.matches(new AbstractModuleDescriptor<Object>()
        {
            @Override
            public Object getModule()
            {
                throw new UnsupportedOperationException();
            }
        }));
    }

    private static class ModuleDescriptorStubA extends AbstractModuleDescriptor<Object>
    {
        @Override
        public Object getModule()
        {
            return null;
        }
    }

    private static class ModuleDescriptorStubB extends ModuleDescriptorStubA
    {}
}
