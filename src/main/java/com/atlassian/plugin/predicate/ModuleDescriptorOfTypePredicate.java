package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorOfTypePredicate extends ModuleDescriptorOfClassPredicate
{
    public ModuleDescriptorOfTypePredicate(final ModuleDescriptorFactory moduleDescriptorFactory, final String moduleDescriptorType)
    {
        super(moduleDescriptorFactory != null ? moduleDescriptorFactory.getModuleDescriptorClass(moduleDescriptorType) : null);
    }
}
