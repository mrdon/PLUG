package com.atlassian.plugin;

import java.net.URL;

/**
 * Creates a plugin artifact from a URL
 *
 * @since 2.1.0
 */
public interface PluginArtifactFactory
{
    /**
     * Creates a plugin artifact
     * @param artifactUrl The artifact URL
     * @return The artifact.  Must not return null
     * @throws IllegalArgumentException If the artifact cannot be created
     */
    PluginArtifact create(URL artifactUrl) throws IllegalArgumentException;
}
