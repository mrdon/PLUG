package com.atlassian.plugin.predicate;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;

/**
 * A {@link ModuleDescriptorPredicate} that matches enabled modules.
 */
public class EnabledModulePredicate implements ModuleDescriptorPredicate
{
    private final PluginAccessor pluginAccessor;

    /**
     * @throws IllegalArgumentException if pluginAccessor is <code>null</code>
     */
    public EnabledModulePredicate(final PluginAccessor pluginAccessor)
    {
        if (pluginAccessor == null)
        {
            throw new IllegalArgumentException("PluginAccessor must not be null when constructing an EnabledModulePredicate!");
        }
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(final ModuleDescriptor moduleDescriptor)
    {
        return moduleDescriptor != null && pluginAccessor.isPluginModuleEnabled(moduleDescriptor.getCompleteKey());
    }
}
