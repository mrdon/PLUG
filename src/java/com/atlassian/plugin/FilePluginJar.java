package com.atlassian.plugin;

import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.io.*;

/**
 * The implementation of PluginJar which reads from a specified file on disk.
 *
 * @see PluginJar
 */
public class FilePluginJar implements PluginJar
{
    private final File jarFile;

    public FilePluginJar(File jarFile)
    {
        this.jarFile = jarFile;
    }

    public InputStream getFile(String fileName) throws PluginParseException
    {
        JarFile jar;
        try
        {
            jar = new JarFile(jarFile);
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot open JAR file for reading: " + jarFile, e);
        }

        ZipEntry entry = jar.getEntry(fileName);
        if (entry == null)
        {
            throw new PluginParseException("File " + fileName + " not found in plugin JAR [" + jarFile + "]");
        }

        InputStream descriptorStream;
        try
        {
            descriptorStream = jar.getInputStream(entry);
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin JAR [" + jarFile + "]", e);
        }
        return descriptorStream;
    }

    public String getFileName()
    {
        return jarFile.getName();
    }

    /**
     * @return a buffered file input stream of the file on disk. This input stream
     * is not resettable.
     */
    public InputStream getInputStream()
    {
        try
        {
            return new BufferedInputStream(new FileInputStream(jarFile));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("Could not open JAR file for reading: " + jarFile, e);
        }
    }
}
