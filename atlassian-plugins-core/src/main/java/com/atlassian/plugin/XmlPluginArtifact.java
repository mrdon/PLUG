package com.atlassian.plugin;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.*;

/**
 * An XML plugin artifact that is just the atlassian-plugin.xml file
 *
 * @since 2.1.0
 */
public class XmlPluginArtifact implements PluginArtifact
{
    private final DeploymentUnit deploymentUnit;

    /**
     * @since 2.9.0
     * @param deploymentUnit
     */
    public XmlPluginArtifact(final DeploymentUnit deploymentUnit)
    {
        this.deploymentUnit = deploymentUnit;
    }

    /**
     * @deprecated use {@link #XmlPluginArtifact(DeploymentUnit)}
     * @param file to deploy
     */
    public XmlPluginArtifact(final File file)
    {
        this.deploymentUnit = new DeploymentUnit(file);
    }

    public DeploymentUnit getDeploymentUnit()
    {
        return deploymentUnit;
    }

    /**
     * Always returns false, since it doesn't make sense for an XML artifact
     */
    public boolean doesResourceExist(String name)
    {
        return false;
    }

    /**
     * Always returns null, since it doesn't make sense for an XML artifact
     */
    public InputStream getResourceAsStream(String name) throws PluginParseException
    {
        return null;
    }

    public String getName()
    {
        return deploymentUnit.getPath().getName();
    }

    /**
     * @return a buffered file input stream of the file on disk. This input stream
     * is not resettable.
     */
    public InputStream getInputStream()
    {
        try
        {
            return new BufferedInputStream(new FileInputStream(deploymentUnit.getPath()));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("Could not find XML file for eading: " + deploymentUnit.getPath(), e);
        }
    }

    public File toFile()
    {
        return deploymentUnit.getPath();
    }
}
