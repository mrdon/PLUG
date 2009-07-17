package com.atlassian.plugin;

/**
 * Controls the life-cycle of the plugin system.
 * 
 * @since 2.2.0
 */
public interface PluginSystemLifecycle
{
    /**
     * Initialise the plugin system. This <b>must</b> be called before anything else.
     * @throws PluginParseException If parsing the plugins failed.
     */
    void init() throws PluginParseException;

    /**
     * Destroys the plugin manager. This <b>must</b> be called when getting rid of the manager instance and you
     * plan to create another one. Failure to do so will leave around significant resources including threads
     * and memory usage and can interfere with a web-application being correctly shutdown.
     * 
     * @since 2.0.0
     */
    void shutdown();

    /**
     * Restart all plugins by disabling and enabling them in the order they were loaded (by plugin loader)
     *
     * @since 2.3.0
     */
    void warmRestart();
}
