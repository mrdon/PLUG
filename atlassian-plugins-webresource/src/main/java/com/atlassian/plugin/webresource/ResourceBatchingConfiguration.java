package com.atlassian.plugin.webresource;

import java.util.List;

/**
 *
 */
public interface ResourceBatchingConfiguration
{
    boolean isSuperBatchingEnabled();

    List<String> getSuperBatchModuleCompleteKeys();
}
