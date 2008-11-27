package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;

/**
 * A {@link ModuleDescriptorPredicate} that matches enabled modules.
 */
public class EnabledModulePredicate<T> implements ModuleDescriptorPredicate<T>
{
    private final PluginAccessor<T> pluginAccessor;

    /**
     * @throws IllegalArgumentException if pluginAccessor is <code>null</code>
     */
    public EnabledModulePredicate(final PluginAccessor<T> pluginAccessor)
    {
        if (pluginAccessor == null)
        {
            throw new IllegalArgumentException("PluginAccessor must not be null when constructing an EnabledModulePredicate!");
        }
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(final ModuleDescriptor<? extends T> moduleDescriptor)
    {
        return (moduleDescriptor != null) && pluginAccessor.isPluginModuleEnabled(moduleDescriptor.getCompleteKey());
    }
}
