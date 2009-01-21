package com.atlassian.plugin;

import java.util.Map;

/**
 * Interface that represents a state of plugins and its modules in the system (enabled/disabled).
 * @author anatoli
 *
 */
public interface PluginManagerState
{
    /**
     * Get the state of a given plugin.
     */
    Boolean getState(final String key);

    /**
     * Get the map of all states.
     */
    Map<String, Boolean> getMap();

    /**
     * Whether or not a plugin is enabled, calculated from it's current state AND default state.
     */
    boolean isEnabled(final Plugin plugin);

    /**
     * Whether or not a given plugin module is enabled in this state, calculated from it's current state AND default state.
     */
    boolean isEnabled(final ModuleDescriptor<?> pluginModule);

    /**
     * Get state map of the given plugin and its modules
     * @param plugin
     * @return
     */
    Map<String, Boolean> getPluginStateMap(final Plugin plugin);

}