package com.atlassian.plugin.webresource;

import java.util.List;

/**
 * Resource batching configuration for the {@link WebResourceManagerImpl}.
 *
 * Currently contains the configuration for super batch support.
 */
public interface ResourceBatchingConfiguration
{
    boolean isSuperBatchingEnabled();

    List<String> getSuperBatchModuleCompleteKeys();
}
