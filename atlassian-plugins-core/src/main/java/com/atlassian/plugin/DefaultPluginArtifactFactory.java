package com.atlassian.plugin;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.File;
import java.net.URI;

/**
 * Creates plugin artifacts by handling URI's that are files and looking at the file's extension
 *
 * @since 2.1.0
 */
public class DefaultPluginArtifactFactory implements PluginArtifactFactory
{
    public PluginArtifact create(final DeploymentUnit deploymentUnit) throws IllegalArgumentException
    {
        PluginArtifact artifact = null;

        URI artifactUri = deploymentUnit.getPath().toURI();

        String protocol = artifactUri.getScheme();

        if ("file".equalsIgnoreCase(protocol))
        {
            File artifactFile = new File(artifactUri);

            String file = artifactFile.getName();
            if (file.endsWith(".jar"))
                artifact = new JarPluginArtifact(deploymentUnit);
            else if (file.endsWith(".xml"))
                artifact = new XmlPluginArtifact(deploymentUnit);
        }

        if (artifact == null)
            throw new IllegalArgumentException("The artifact URI " + artifactUri + " is not a valid plugin artifact");

        return artifact;
    }


}
