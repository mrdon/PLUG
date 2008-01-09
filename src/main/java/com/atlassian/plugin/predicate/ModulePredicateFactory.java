package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.ModuleDescriptorFactory;

/**
 * A convenient class to create some module predicates.
 */
public class ModulePredicateFactory
{
    public static ModulePredicate getEnabledModuleOfClassPredicate(final PluginAccessor pluginAccessor, final Class moduleClass)
    {
        return new AndModulePredicate(new EnabledModulePredicate(pluginAccessor), new ModuleClassModulePredicate(moduleClass));
    }

    public static ModulePredicate getEnabledModuleOfClassAndDescriptorClassPredicate(final PluginAccessor pluginAccessor, final Class moduleClass, final Class moduleDescriptorClass)
    {
        return new AndModulePredicate(new EnabledModulePredicate(pluginAccessor), new ModuleClassModulePredicate(moduleClass), new ModuleDescriptorClassModulePredicate(moduleDescriptorClass));
    }

    public static ModulePredicate getEnabledModuleOfClassAndDescriptorClassPredicate(final PluginAccessor pluginAccessor, final Class moduleClass, final Class[] moduleDescriptorClasses)
    {
        return new AndModulePredicate(new EnabledModulePredicate(pluginAccessor), new ModuleClassModulePredicate(moduleClass), new ModuleDescriptorClassModulePredicate(moduleDescriptorClasses));
    }

    public static ModulePredicate getEnabledModuleWithDescriptorClass(final PluginAccessor pluginAccessor, final Class moduleDescriptorClass)
    {
        return new AndModulePredicate(new EnabledModulePredicate(pluginAccessor), new ModuleDescriptorClassModulePredicate(moduleDescriptorClass));
    }

    public static ModulePredicate getEnabledModuleWithDescriptorType(final PluginAccessor pluginAccessor, final ModuleDescriptorFactory moduleDescriptorFactory, final String moduleDescriptorType)
    {
        return new AndModulePredicate(new EnabledModulePredicate(pluginAccessor), new ModuleDescriptorTypeModulePredicate(moduleDescriptorFactory, moduleDescriptorType));
    }

    private static class AndModulePredicate implements ModulePredicate
    {
        private final ModulePredicate predicate1;
        private final ModulePredicate predicate2;
        private final ModulePredicate predicate3;

        public AndModulePredicate(final ModulePredicate predicate1, final ModulePredicate predicate2)
        {
            this(predicate1, predicate2, null);
        }

        public AndModulePredicate(final ModulePredicate predicate1, final ModulePredicate predicate2, final ModulePredicate predicate3)
        {
            this.predicate1 = predicate1;
            this.predicate2 = predicate2;
            this.predicate3 = predicate3;
        }

        public boolean matches(final ModuleDescriptor moduleDescriptor)
        {
            return (predicate1 == null || predicate1.matches(moduleDescriptor))
                && (predicate2 == null || predicate2.matches(moduleDescriptor))
                && (predicate3 == null || predicate3.matches(moduleDescriptor));
        }
    }
}
