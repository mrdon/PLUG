package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections.Predicate;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is an instance of one of the given {@link Class}.
 */
public class ModuleDescriptorOfClassPredicate implements ModuleDescriptorPredicate
{
    private final Collection moduleDescriptorClasses;

    public ModuleDescriptorOfClassPredicate(final Class moduleDescriptorClass)
    {
        moduleDescriptorClasses = Collections.singleton(moduleDescriptorClass);
    }

    /**
     * @throws IllegalArgumentException if the moduleDescriptorClasses is <code>null</code>
     */
    public ModuleDescriptorOfClassPredicate(final Class[] moduleDescriptorClasses)
    {
        if (moduleDescriptorClasses == null)
        {
            throw new IllegalArgumentException("Module descriptor classes array should not be null when constructing ModuleOfClassPredicate!");
        }
        this.moduleDescriptorClasses = Arrays.asList(moduleDescriptorClasses);
    }

    public boolean matches(final ModuleDescriptor moduleDescriptor)
    {
        return moduleDescriptor != null && CollectionUtils.exists(moduleDescriptorClasses, new Predicate()
        {
            public boolean evaluate(Object object)
            {
                return object != null && ((Class) object).isInstance(moduleDescriptor);
            }
        });
    }
}