package com.atlassian.plugin;

/**
 * Interface to control the state of the plugin system
 */
public interface PluginController
{
    /**
     * Enable a plugin by key.
     */
    void enablePlugin(String key);

    /**
     * Disable a plugin by key.
     */
    void disablePlugin(String key);

    /**
     * Enable a plugin module by key.
     */
    void enablePluginModule(String completeKey);

    /**
     * Disable a plugin module by key.
     */
    void disablePluginModule(String completeKey);

    /**
     * Installs a plugin and returns the plugin key
     * @throws com.atlassian.plugin.PluginParseException if the plugin is not a valid plugin
     */
    String installPlugin(PluginJar pluginJar) throws PluginParseException;

    /**
     * Uninstall the plugin, disabling it first.
     * @throws PluginException if there was some problem uninstalling the plugin.
     */
    void uninstall(Plugin plugin) throws PluginException;

    /**
     * Search all loaders and add any new plugins you find.
     * @return The number of new plugins found.
     */
    int scanForNewPlugins() throws PluginParseException;
}
