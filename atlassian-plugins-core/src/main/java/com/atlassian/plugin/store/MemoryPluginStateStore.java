package com.atlassian.plugin.store;

import com.atlassian.plugin.PluginManagerState;
import com.atlassian.plugin.PluginStateStore;

/**
 * A basic plugin state store that stores state in memory. Not recommended for production use.
 */
public class MemoryPluginStateStore implements PluginStateStore
{
    private final PluginManagerState state = new PluginManagerState();

    public void savePluginState(final PluginManagerState state)
    {
        this.state.setState(state);
    }

    public PluginManagerState loadPluginState()
    {
        return new PluginManagerState(state);
    }
}
