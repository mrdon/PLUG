package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * A {@link ModulePredicate} that matches modules that are is an instance of the given {@link Class}.
 */
public class ModuleClassModulePredicate implements ModulePredicate
{
    private final Class moduleClass;

    /**
     * @throws IllegalArgumentException if the moduleClass is <code>null</code>
     */
    public ModuleClassModulePredicate(final Class moduleClass)
    {
        if (moduleClass == null)
        {
            throw new IllegalArgumentException("Module class should not be null when constructing ModuleClassModulePredicate!");
        }
        this.moduleClass = moduleClass;
    }

    public boolean matches(final ModuleDescriptor moduleDescriptor)
    {
        if (moduleDescriptor != null)
        {
            final Class moduleClassInDescriptor = moduleDescriptor.getModuleClass();
            return moduleClassInDescriptor != null && moduleClass.isAssignableFrom(moduleClassInDescriptor);
        }

        return false;
    }
}
