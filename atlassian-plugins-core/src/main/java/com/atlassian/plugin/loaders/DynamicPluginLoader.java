package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginJar;
import com.atlassian.plugin.PluginParseException;

/**
 * Plugin loader that supports installed plugins at runtime
 */
public interface DynamicPluginLoader extends PluginLoader
{
    /**
     * Determines if this loader can load the jar.
     * @param pluginJar The jar to test
     * @return The plugin key, null if it cannot load the jar
     * @throws com.atlassian.plugin.PluginParseException If there are exceptions parsing the plugin configuration
     */
    String canLoad(PluginJar pluginJar) throws PluginParseException;
}
