package com.atlassian.plugin.module;

import com.atlassian.plugin.Plugin;

/**
 * A plugin that is managed by a container
 *
 * @since 2.5.0
 */
public interface ContainerManagedPlugin extends Plugin
{
    /**
     * @return The object to use to access the plugin's container
     */
    ContainerAccessor getContainerAccessor();
}
