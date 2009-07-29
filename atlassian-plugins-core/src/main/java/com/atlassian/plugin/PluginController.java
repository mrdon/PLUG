package com.atlassian.plugin;

import java.util.List;
import java.util.Set;

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
     * @param pluginArtifact The plugin artifact to install
     * @throws com.atlassian.plugin.PluginParseException if the plugin is not a valid plugin
     * @return The plugin key
     * @deprecated Since 2.3.0, use {@link #installPlugins(PluginArtifact[])} instead
     */
    String installPlugin(PluginArtifact pluginArtifact) throws PluginParseException;

    /**
     * Installs multiple plugins and returns the list of plugin keys.  All plugin artifacts must be for valid plugins
     * or none will be installed.
     *
     * @param pluginArtifacts The list of plugin artifacts to install
     * @return A list of plugin keys
     * @throws com.atlassian.plugin.PluginParseException if any plugin is not a valid plugin
     * @since 2.3.0
     */
    Set<String> installPlugins(PluginArtifact... pluginArtifacts) throws PluginParseException;

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
