package com.atlassian.plugin;

/**
 * A place to store plugins which can be installed and uninstalled.
 */
public interface PluginInstaller
{
    /**
     * Installs the plugin with the given key. If the JAR filename
     * already exists, it is replaced silently.
     *
     * @see PluginJar#getFileName() 
     */
    void installPlugin(String key, PluginJar pluginJar) throws PluginParseException;
}
