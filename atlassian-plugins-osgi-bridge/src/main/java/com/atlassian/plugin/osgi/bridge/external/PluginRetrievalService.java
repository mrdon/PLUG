package com.atlassian.plugin.osgi.bridge.external;

import com.atlassian.plugin.Plugin;

/**
 * Provides access for the plugin the consuming bundle is a part of
 *
 * @since 2.6
 */
public interface PluginRetrievalService
{
    /**
     * @return the plugin of the service consumer that can be cached for the life of the plugin.
     *         Can be null if there is no corresponding {@Plugin} instance for the consuming bundle, as would be the
     *         case for a framework bundle, for example.
     */
    Plugin getPlugin();
}
