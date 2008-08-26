package com.atlassian.plugin.event.events;

import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginAccessor;

/**
 * Event that signifies the plugin framework has been started and initialized
 */
public class PluginFrameworkStartedEvent
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;

    public PluginFrameworkStartedEvent(PluginController pluginController, PluginAccessor pluginAccessor)
    {
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
    }

    public PluginController getPluginController()
    {
        return pluginController;
    }

    public PluginAccessor getPluginAccessor()
    {
        return pluginAccessor;
    }
}
