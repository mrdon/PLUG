package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event that signifies a plugin has been enabled for a particular tenant, (it should be already enabled in the plugin
 * system)
 */
public class MultiTenantPluginEnabledEvent
{
    private final Plugin plugin;

    public MultiTenantPluginEnabledEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }
}