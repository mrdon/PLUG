package com.atlassian.labs.plugins3;

import com.atlassian.labs.plugins3.api.annotation.Component;
import com.atlassian.labs.plugins3.api.annotation.HostComponent;
import com.atlassian.plugin.PluginAccessor;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ScannedComponent
{
    private final PluginAccessor pluginAccessor;

    @Inject
    public ScannedComponent(@HostComponent PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public String getStatus()
    {
        return "Scan Successful: " + pluginAccessor;
    }
}
