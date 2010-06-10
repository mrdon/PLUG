package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event that signifies a plugin has been disabled for a particular tenant, but not for the entire plugin system
 */
public class MultiTenantPluginDisabledEvent
{
    private final Plugin plugin;

    public MultiTenantPluginDisabledEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }
}
