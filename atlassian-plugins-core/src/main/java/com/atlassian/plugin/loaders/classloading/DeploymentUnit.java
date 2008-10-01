package com.atlassian.plugin.loaders.classloading;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.artifact.AbstractFilePluginArtifact;

import java.io.File;
import java.io.InputStream;

/**
 * @deprecated Since 2.1.0, use {@link PluginArtifact} instead
 */
public class DeploymentUnit extends AbstractFilePluginArtifact
{
    public DeploymentUnit(File path)
	{
        super(path);
    }

    public DeploymentUnit(PluginArtifact artifact)
	{
        super(artifact.getFile(), artifact.lastModified());
    }

    public File getPath()
    {
        return getFile();
    }

    public InputStream getResourceAsStream(String name) throws PluginParseException
    {
        return null;
    }

}
