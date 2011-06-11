package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

/**
 * @see {@link PluginArtifact} {@link OsgiPluginFactory}
 * @since 2.9.0
 */
public class OsgiPluginArtifact extends JarPluginArtifact
{
    private final DeploymentUnit originalDeploymentUnit;

    public OsgiPluginArtifact(final DeploymentUnit deploymentUnit, final DeploymentUnit originalDeploymentUnit)
    {
        super(deploymentUnit);
        this.originalDeploymentUnit = originalDeploymentUnit;
    }

    /**
     * @since 2.9.0
     * @return deploymentUnit of the original unprocessed plugin jar
     */
    public DeploymentUnit getOriginalDeploymentUnit()
    {
        return originalDeploymentUnit;
    }
}
