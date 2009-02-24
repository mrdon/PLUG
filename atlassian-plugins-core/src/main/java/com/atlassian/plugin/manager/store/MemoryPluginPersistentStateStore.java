package com.atlassian.plugin.manager.store;

import com.atlassian.plugin.manager.DefaultPluginPersistentState;
import com.atlassian.plugin.manager.PluginPersistentState;
import com.atlassian.plugin.manager.PluginPersistentStateStore;

/**
 * A basic plugin state store that stores state in memory. Not recommended for production use.
 */
public class MemoryPluginPersistentStateStore implements PluginPersistentStateStore
{
    private final DefaultPluginPersistentState state = new DefaultPluginPersistentState();

    public void save(final PluginPersistentState state)
    {
        this.state.setState(state);
    }

    public PluginPersistentState load()
    {
        return state;
    }
}
