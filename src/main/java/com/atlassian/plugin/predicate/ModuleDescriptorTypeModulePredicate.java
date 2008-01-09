package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModulePredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorTypeModulePredicate extends ModuleDescriptorClassModulePredicate
{
    public ModuleDescriptorTypeModulePredicate(final ModuleDescriptorFactory moduleDescriptorFactory, final String moduleDescriptorType)
    {
        super(moduleDescriptorFactory != null ? moduleDescriptorFactory.getModuleDescriptorClass(moduleDescriptorType) : null);
    }
}
