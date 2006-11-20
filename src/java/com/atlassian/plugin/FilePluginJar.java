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

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public InputStream getFile(String fileName) throws PluginParseException
    {
        final JarFile jar;
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
