package com.atlassian.plugin;

/**
 * A basic plugin state store that stores state in memory. Not recommended for production use.
 */
public class MemoryPluginStateStore implements PluginStateStore
{
    private PluginManagerState state = new PluginManagerState();

    public void savePluginState(PluginManagerState state)
    {
        this.state = state;
    }

    public PluginManagerState loadPluginState()
    {
        return state;
    }
}
