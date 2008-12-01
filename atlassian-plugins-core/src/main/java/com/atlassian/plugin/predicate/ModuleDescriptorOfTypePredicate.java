package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorOfTypePredicate<T, M extends T> extends ModuleDescriptorOfClassPredicate<M>
{
    public ModuleDescriptorOfTypePredicate(final ModuleDescriptorFactory<T, ModuleDescriptor<? extends T>> moduleDescriptorFactory, final String moduleDescriptorType)
    {
        super(Coerce.<T, M> getModuleDescriptor(moduleDescriptorFactory, moduleDescriptorType));
    }

    /**
     * Required to correctly type the super constructor call.
     */
    private static class Coerce
    {
        private static <T, M extends T> Class<? extends ModuleDescriptor<? extends M>> getModuleDescriptor(final ModuleDescriptorFactory<T, ModuleDescriptor<? extends T>> moduleDescriptorFactory, final String moduleDescriptorType)
        {
            final Class<ModuleDescriptor<M>> moduleDescriptorClass = moduleDescriptorFactory.<ModuleDescriptor<M>> getModuleDescriptorClass(moduleDescriptorType);
            return moduleDescriptorClass;
        }
    }
}
