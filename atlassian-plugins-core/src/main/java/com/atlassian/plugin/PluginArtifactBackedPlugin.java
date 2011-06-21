package com.atlassian.plugin;

import com.atlassian.plugin.factories.PluginFactory;

/**
 * Applied to {@link Plugin} implementations which are produced by {@link PluginFactory}s that are backed by {@link PluginArtifact}s
 * @since 2.9.0
 */
public interface PluginArtifactBackedPlugin extends Plugin
{
    /**
     * @return the original, unprocessed or transformed {@link PluginArtifact} used to create this plugin instance.
     * @since 2.9.0
     */
    PluginArtifact getPluginArtifact();
}
