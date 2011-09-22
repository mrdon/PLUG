package com.atlassian.plugin.metadata;

import java.util.Set;

/**
 * Provides the keys for both plugins and modules that have
 * been marked as required by the host application.
 *
 * @since 2.6.6
 */
public interface RequiredPluginProvider
{
    /**
     * The set of all of the plugins that have been marked as required by the host application.
     *
     * @return The set of plugins required by the host application, or the empty set.
     */
    Set<String> getRequiredPluginKeys();

    /**
     * The set of all of the plugin modules that have been marked as required by the host application.
     *
     * @return The set of plugin modules required by the host application, or the empty set.
     */
    Set<String> getRequiredModuleKeys();
}
