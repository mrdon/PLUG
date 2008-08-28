package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

public class PluginEnabledEvent
{
    private final Plugin plugin;

    public PluginEnabledEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }
}
