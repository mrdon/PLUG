package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules that are is an instance of the given {@link Class}.
 */
public class ModuleOfClassPredicate<T> implements ModuleDescriptorPredicate<T>
{
    private final Class<? extends T> moduleClass;

    /**
     * @throws IllegalArgumentException if the moduleClass is <code>null</code>
     */
    public ModuleOfClassPredicate(final Class<? extends T> moduleClass)
    {
        if (moduleClass == null)
        {
            throw new IllegalArgumentException("Module class should not be null when constructing ModuleOfClassPredicate!");
        }
        this.moduleClass = moduleClass;
    }

    public boolean matches(final ModuleDescriptor<? extends T> moduleDescriptor)
    {
        if (moduleDescriptor != null)
        {
            final Class<? extends T> moduleClassInDescriptor = moduleDescriptor.getModuleClass();
            return (moduleClassInDescriptor != null) && moduleClass.isAssignableFrom(moduleClassInDescriptor);
        }

        return false;
    }
}
