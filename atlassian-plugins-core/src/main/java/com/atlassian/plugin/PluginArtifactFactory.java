package com.atlassian.plugin;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.net.URI;

/**
 * Creates a plugin artifact from a URL
 *
 * @since 2.1.0
 */
public interface PluginArtifactFactory
{
    /**
     * Creates a plugin artifact
     * @param deploymentUnit of the artifact
     * @return The artifact.  Must not return null
     * @throws IllegalArgumentException If the artifact cannot be created
     */
    PluginArtifact create(DeploymentUnit deploymentUnit) throws IllegalArgumentException;
}
