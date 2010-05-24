package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event fired when a plugin is enabled, installed or updated.
 *
 * @see com.atlassian.plugin.event.events
 */
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
