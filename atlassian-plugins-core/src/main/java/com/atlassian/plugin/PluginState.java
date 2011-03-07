package com.atlassian.plugin;

/**
 * Represents the state of the plugin
 *
 * @since 2.2.0
 */
public enum PluginState
{
    /**
     * The plugin has been installed into the plugin system
     */
    INSTALLED,

    /**
     * The plugin is in the process of being enabled
     */
    ENABLING,

    /**
     * The plugin has been enabled
     */
    ENABLED,

    /**
     * The plugin is in the process of being disabled
     *
     * @since 2.8.0
     */
    DISABLING,

    /**
     * The plugin has been disabled
     */
    DISABLED,

    /**
     * The plugin has been uninstalled and should be unavailable
     */
    UNINSTALLED
}
