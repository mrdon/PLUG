package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event fired when a plugin is explicited uninstalled (as opposed to as part of an upgrade).
 *
 * @since 2.5
 */
public class PluginUninstalledEvent
{
    private final Plugin plugin;

    public PluginUninstalledEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }
}
