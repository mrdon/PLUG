package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;

/**
 * Provides information about plugins and modules that is application host specific. The information is not relevant to
 * the plugins system but may be relevant to managing the plugins.
 *
 * @since 2.6.0
 */
public interface PluginMetadataManager
{
    /**
     * This is used to determine if the plugin was provided by the host application or provided by a user.
     *
     * @param plugin used to determine the state, not null.
     * @return true if the plugin was not provided by the host application, false otherwise.
     */
    boolean isUserInstalled(Plugin plugin);

    /**
     * This is used to determine if a plugin is considered optional. If an optional plugin is disabled it should not
     * adversely effect the host application. If any {@link com.atlassian.plugin.ModuleDescriptor}'s are not optional
     * then the plugin is also not optional.
     *
     * @param plugin used to determine the state, not null.
     * @return true if the plugin can safely be disabled, false if the plugin being disabled would adversely effect the
     * host application.
     */
    boolean isOptional(Plugin plugin);

    /**
     * This is used to determine if a module is considered optional. If an optional module is disabled it should not
     * adversely effect the host application. A module can not be optional if its containing plugin is not optional.
     *
     * @param moduleDescriptor used to determine state, not null.
     * @return true if the module can safely be disabled, false if the module being disabled would adversely effect the
     * host application.
     */
    boolean isOptional(ModuleDescriptor moduleDescriptor);
}
