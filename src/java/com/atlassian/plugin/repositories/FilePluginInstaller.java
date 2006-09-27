package com.atlassian.plugin.repositories;

import com.atlassian.plugin.PluginInstaller;
import com.atlassian.plugin.PluginJar;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.FileUtils;

import java.io.*;

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
    public void installPlugin(String key, PluginJar pluginJar) throws PluginParseException
    {
        File newPluginFile = new File(directory, pluginJar.getFileName());
        if (newPluginFile.exists())
            newPluginFile.delete();

        OutputStream os = null;
        try
        {
            os= new FileOutputStream(newPluginFile);
            FileUtils.copy(pluginJar.getInputStream(), os);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not install plugin: " + pluginJar, e);
        }
        finally
        {
            FileUtils.shutdownStream(os);
        }
    }
}
