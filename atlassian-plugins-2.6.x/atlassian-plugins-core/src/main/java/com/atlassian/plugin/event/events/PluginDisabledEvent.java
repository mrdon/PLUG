package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event that signifies a plugin has been disabled, uninstalled or updated.
 *
 * @see com.atlassian.plugin.event.events
 */
public class PluginDisabledEvent
{
    private final Plugin plugin;
    
    public PluginDisabledEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }
    
    public Plugin getPlugin()
    {
        return plugin;
    }
}
