package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorOfTypePredicate<T, M extends ModuleDescriptor<T>> extends ModuleDescriptorOfClassPredicate<T>
{
    public ModuleDescriptorOfTypePredicate(final ModuleDescriptorFactory<T, ? extends ModuleDescriptor<? extends T>> moduleDescriptorFactory, final String moduleDescriptorType)
    {
        super(moduleDescriptorFactory != null ? moduleDescriptorFactory.getModuleDescriptorClass(moduleDescriptorType) : null);
    }
}
