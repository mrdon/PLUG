package com.atlassian.plugin;

/**
 * Makes a plugin module aware of its activation state. Modules should implement this
 * interface if they want to be notified when they are enabled and disabled.
 */
public interface StateAware
{
    /**
     * Called by the plugin manager when the module is activated. Modules that are active
     * when the plugin manager is initialised will have this method called at that time.
     */
    void enabled();

    /**
     * Called by the plugin manager when the module is deactivated. This method will only
     * be called if the plugin is deactivated while the application is running: stopping
     * the server will <i>not</i> cause this method to be called on any plugins.
     */
    void disabled();
}
