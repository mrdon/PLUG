package com.atlassian.plugin.webresource;

import java.util.List;

/**
 * Resource batching configuration for the {@link WebResourceManagerImpl}.
 *
 * Currently contains the configuration for batch support.
 */
public interface ResourceBatchingConfiguration
{
    /**
     * Gets whether web resources in different resource modules should be batched together.
     * @return true if web resources in different resource modules should be batched together
     */
    boolean isSuperBatchingEnabled();

    /**
     * Gets the list of resource plugin modules that should be included in the superbatch, in the order that
     * they should be batched. No dependency resolution is performed, so it is important that the configuration
     * includes all dependent resources in the right order.
     *
     * Any call to {@link WebResourceManager#requireResource} for one of these resources will be a no-op,
     * and any dependency resolution for resources will stop if the dependency is in the superbatch.
     * @return a list of resource plugin modules that should be included in the superbatch.
     */
    List<String> getSuperBatchModuleCompleteKeys();

    /**
     * Determines whether web resources in the same context should be batched together.
     * @return true if web resources in the same context are batched together.
     * @since 2.9.0
     */
    boolean isContextBatchingEnabled();

    /**
     * Determines whether plugin resources of the same type defined within a single web resource
     * are batched into one file.
     * @return true if plugin resources in the same web resource are batched together.
     * @since 2.9.0
     */
    boolean isPluginWebResourceBatchingEnabled();
}
