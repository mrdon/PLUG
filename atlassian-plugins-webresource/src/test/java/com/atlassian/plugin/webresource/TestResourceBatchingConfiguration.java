package com.atlassian.plugin.webresource;

import java.util.List;
import java.util.Arrays;

/**
 *
 */
class TestResourceBatchingConfiguration implements ResourceBatchingConfiguration
{
    public boolean enabled = false;

    public boolean isSuperBatchingEnabled()
    {
        return enabled;
    }

    public List<String> getSuperBatchModuleCompleteKeys()
    {
        return Arrays.asList(
                "test.atlassian:superbatch",
                "test.atlassian:superbatch2",
                "test.atlassian:missing-plugin"
        );
    }
}
