package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginArtifact;

import java.net.URL;
import java.io.File;

/**
 * Creates a plugin artifact from a URL
 *
 * @since 2.1.0
 */
public interface PluginArtifactFactory
{
    /**
     * Creates a plugin artifact
     * @param artifactFile The artifact URL
     * @return The artifact.  Must not return null
     * @throws IllegalArgumentException If the artifact cannot be created
     */
    PluginArtifact create(File artifactFile) throws IllegalArgumentException;
}
