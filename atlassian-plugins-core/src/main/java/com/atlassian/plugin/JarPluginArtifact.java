package com.atlassian.plugin;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.IOUtils;

import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.io.*;

/**
 * The implementation of PluginArtifact that is backed by a jar file.
 *
 * @see PluginArtifact
 * @since 2.0.0
 */
public class JarPluginArtifact implements PluginArtifact
{
    private final DeploymentUnit deploymentUnit;

    /**
     * @since 2.9.0
     * @param deploymentUnit
     */
    public JarPluginArtifact(final DeploymentUnit deploymentUnit)
    {
        this.deploymentUnit = deploymentUnit;
    }

    /**
     * @deprecated use {@link #JarPluginArtifact(DeploymentUnit)}
     * @param file
     */
    public JarPluginArtifact(final File file)
    {
        this.deploymentUnit = new DeploymentUnit(file);
    }

    public DeploymentUnit getDeploymentUnit()
    {
        return deploymentUnit;
    }

    public boolean doesResourceExist(String name)
    {
        InputStream in = null;
        try
        {
            in = getResourceAsStream(name);
            return (in != null);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public InputStream getResourceAsStream(String fileName) throws PluginParseException
    {
        Validate.notNull(fileName, "The file name must not be null");
        final JarFile jar;
        final ZipEntry entry;
        try
        {
            jar = new JarFile(deploymentUnit.getPath());
            entry = jar.getEntry(fileName);
            if (entry == null)
            {
                jar.close();
                return null;
            }
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot open JAR file for reading: " + deploymentUnit.getPath(), e);
        }

        InputStream descriptorStream;
        try
        {
            descriptorStream = new BufferedInputStream(jar.getInputStream(entry)) {

                // because we do not expose a handle to the jar file this stream is associated with, we need to make sure
                // we explicitly close the jar file when we're done with the stream (else we'll have a file handle leak)
                public void close() throws IOException
                {
                    super.close();
                    jar.close();
                }
            };
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin JAR [" + deploymentUnit.getPath() + "]", e);
        }
        return descriptorStream;
    }

    public String getName()
    {
        return deploymentUnit.getPath().getName();
    }

    @Override
    public String toString()
    {
        return getName();
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
            throw new RuntimeException("Could not open JAR file for reading: " + deploymentUnit.getPath(), e);
        }
    }

    public File toFile()
    {
        return deploymentUnit.getPath();
    }
}
