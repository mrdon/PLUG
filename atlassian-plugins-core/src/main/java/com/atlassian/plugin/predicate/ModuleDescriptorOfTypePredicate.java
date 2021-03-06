package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorOfTypePredicate<M> extends ModuleDescriptorOfClassPredicate<M>
{
    @SuppressWarnings("unchecked")
    public ModuleDescriptorOfTypePredicate(final ModuleDescriptorFactory moduleDescriptorFactory, final String moduleDescriptorType)
    {
        super((Class<? extends ModuleDescriptor<? extends M>>) moduleDescriptorFactory.getModuleDescriptorClass(moduleDescriptorType));
    }
}
