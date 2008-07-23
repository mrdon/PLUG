package com.atlassian.plugin.repositories;

import com.atlassian.plugin.PluginInstaller;
import com.atlassian.plugin.PluginArtifact;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple implementation of a PluginInstaller which writes plugin JARs
 * to a specified directory.
 *
 * @see PluginInstaller
 */
public class FilePluginInstaller implements PluginInstaller
{
    private File directory;

    /**
     * @param directory where plugin JARs will be installed.
     */
    public FilePluginInstaller(File directory)
    {
        this.directory = directory;
    }

    /**
     * If there is an existing JAR with the same filename, it is replaced.
     *
     * @throws RuntimeException if there was an exception reading or writing files.
     */
    public void installPlugin(String key, PluginArtifact pluginArtifact)
    {
        File newPluginFile = new File(directory, pluginArtifact.getFileName());
        if (newPluginFile.exists())
            newPluginFile.delete();

        OutputStream os = null;
        try
        {
            os= new FileOutputStream(newPluginFile);
            IOUtils.copy(pluginArtifact.getInputStream(), os);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not install plugin: " + pluginArtifact, e);
        }
        finally
        {
            IOUtils.closeQuietly(os);
        }
    }
}
