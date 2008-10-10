package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;
import org.codehaus.classworlds.uberjar.protocol.jar.NonLockingJarHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * The implementation of PluginArtifact that is backed by a jar file.
 *
 * @see com.atlassian.plugin.PluginArtifact
 * @since 2.0.0
 */
public class JarPluginArtifact extends AbstractFilePluginArtifact
{
    public JarPluginArtifact(File file)
    {
        super(file);
    }

    public Iterable<String> getResourceNames() throws IOException
    {
        JarFile jarFile = getJarFile();
        try
        {
            Collection<String> entryNames = new ArrayList<String>(jarFile.size());
            for (Enumeration en = jarFile.entries(); en.hasMoreElements();)
            {
                Object jarEntry = en.nextElement();
                if (!(jarEntry instanceof JarEntry))
                    throw new RuntimeException("Expected JarEntry, got " + jarEntry.getClass());
                entryNames.add(((JarEntry) jarEntry).getName());
            }
            return entryNames;
        }
        finally
        {
            jarFile.close();
        }
    }

    public URL getResource(String name)
    {
        try
        {
            return new URL(
                new URL("jar:file:" + getFile().getAbsolutePath() + "!/"), name, NonLockingJarHandler.getInstance());
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public InputStream getResourceAsStream(String fileName) throws PluginParseException
    {
        Validate.notNull(fileName, "The file name must not be null");
        final JarFile jar = getJarFile();

        ZipEntry entry = jar.getEntry(fileName);
        if (entry == null)
        {
            throw new PluginParseException("File " + fileName + " not found in plugin JAR [" + getFile() + "]");
        }

        InputStream descriptorStream;
        try
        {
            descriptorStream = new BufferedInputStream(jar.getInputStream(entry))
            {

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
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin JAR [" + getFile() + "]", e);
        }
        return descriptorStream;
    }

    private JarFile getJarFile()
    {
        try
        {
            return new JarFile(getFile());
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot open JAR file for reading: " + getFile(), e);
        }
    }

}