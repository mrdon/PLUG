package com.atlassian.plugin;

import java.util.Collection;

/**
 * To be implemented by all {@link ModuleDescriptor}s that describe a module that can have dependent modules.
 *
 * @since 2.8.0
 */
public interface HasDependentModules {

    /**
     * @param pluginAccessor    a {@link PluginAccessor} to be used to retrieve {@link ModuleDescriptor}s
     * @return a {@link Collection} of the {@link ModuleDescriptor}s for the dependent modules
     */
    Collection<ModuleDescriptor<?>> getDependentModules(PluginAccessor pluginAccessor);

}
