package com.atlassian.plugin;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

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
     * @param artifactUrl The artifact URL
     * @return The created artifact
     * @throws IllegalArgumentException If an artifact cannot be created from the URL
     */
    public PluginArtifact create(URL artifactUrl)
    {
        PluginArtifact artifact = null;
        String protocol = artifactUrl.getProtocol();
        if ("file".equalsIgnoreCase(protocol))
        {
            File artifactFile;
            try
            {
                artifactFile = new File(artifactUrl.toURI());
            } catch (URISyntaxException e)
            {
                throw new IllegalArgumentException("Invalid artifact URI: "+artifactUrl, e);
            }
            String file = artifactUrl.getFile();
            if (file.endsWith(".jar"))
                artifact = new JarPluginArtifact(artifactFile);
            else if (file.endsWith(".xml"))
                artifact = new XmlPluginArtifact(artifactFile);
            else if (artifactFile.isDirectory())
                artifact = new DirectoryPluginArtifact(artifactFile);
        }

        if (artifact == null)
            throw new IllegalArgumentException("The artifact URI "+artifactUrl+" is not a valid plugin artifact");
        
        return artifact;
    }
}
