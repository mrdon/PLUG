package com.atlassian.plugin.event.events;

import com.atlassian.plugin.Plugin;

/**
 * Event that indicates a plugin has been upgraded at runtime
 *
 * @since 2.2.0
 */
public class PluginUpgradedEvent
{
    private final Plugin plugin;

    /**
     * Constructs the event
     * @param plugin The plugin that has been upgraded
     */
    public PluginUpgradedEvent(Plugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @return the plugin that has been upgraded
     */
    public Plugin getPlugin()
    {
        return plugin;
    }
}
