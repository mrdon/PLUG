package com.atlassian.plugin.event.events;

import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginAccessor;

/**
 * Event that signifies the plugin framework is being started
 */
public class PluginFrameworkStartingEvent
{
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;

    public PluginFrameworkStartingEvent(PluginController pluginController, PluginAccessor pluginAccessor)
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