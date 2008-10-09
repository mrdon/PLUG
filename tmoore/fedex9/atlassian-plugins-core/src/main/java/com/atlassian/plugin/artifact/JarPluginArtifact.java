package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public InputStream getResourceAsStream(String fileName) throws PluginParseException
    {
        File jarFile = getFile();
        Validate.notNull(fileName, "The file name must not be null");
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
}