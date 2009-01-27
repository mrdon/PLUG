package com.atlassian.plugin;

import java.util.Map;

/**
 * Interface that represents a state of plugins and its modules in the system (enabled/disabled).
 * 
 * Represents a configuration state for plugins and plugin modules. The configuration state (enabled
 * or disabled) is separate from the plugins and modules themselves because a plugin may have multiple
 * states depending on the context.
 * @author anatoli
 *
 */
public interface PluginManagerState
{
    /**
     * Get the map of all states.
     * @return The map that maps plugins and modules' keys to a state (Boolean.True/Boolean.False). State stored in this map represents only 
     *         the <i>differences</i> between the current state and the default state configured in the plugin(module).
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
     * @return The map that maps the plugin and its modules' keys to plugin state (Boolean.TRUE/Boolean.FALSE). State stored in this map represents only 
     *         the <i>differences</i> between the current state and the default state configured in the plugin(module).
     */
    Map<String, Boolean> getPluginStateMap(final Plugin plugin);

}