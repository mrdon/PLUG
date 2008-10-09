package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginArtifact;

import java.io.File;

/**
 * Creates plugin artifacts by handling URL's that are files and looking at the file's extension
 *
 * @since 2.1.0
 */
public class DefaultPluginArtifactFactory implements PluginArtifactFactory
{
    /**
     * Creates the artifact by looking at the file extension
     *
     * @param artifactFile The artifact URL
     * @return The created artifact
     * @throws IllegalArgumentException If an artifact cannot be created from the URL
     */
    public PluginArtifact create(File artifactFile)
    {
        PluginArtifact artifact = null;

        String file = artifactFile.getName();
        if (file.endsWith(".jar"))
            artifact = new JarPluginArtifact(artifactFile);
        else if (file.endsWith(".xml"))
            artifact = new AtomicPluginArtifact(artifactFile);
        else if (artifactFile.isDirectory())
            artifact = new DirectoryPluginArtifact(artifactFile);

        if (artifact == null)
            throw new IllegalArgumentException("The artifact "+file+" is not a valid plugin artifact");
        
        return artifact;
    }
}
