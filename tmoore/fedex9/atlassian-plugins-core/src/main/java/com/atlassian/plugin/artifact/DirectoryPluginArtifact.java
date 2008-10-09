package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;

import java.io.*;

public class DirectoryPluginArtifact extends AbstractFilePluginArtifact
{
    public DirectoryPluginArtifact(File directory)
    {
        super(directory);
    }

    public InputStream getResourceAsStream(String fileName) throws PluginParseException
    {
        Validate.notNull(fileName, "The file name must not be null");
        File directory = getFile();
        File resource = new File(directory, fileName);
        if (!resource.exists())
        {
            throw new PluginParseException("File " + fileName + " not found in plugin directory [" + directory + "]");
        }

        InputStream descriptorStream;
        try
        {
            descriptorStream = new BufferedInputStream(new FileInputStream(resource));
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin directory [" + directory + "]", e);
        }
        return descriptorStream;

    }

    public InputStream getInputStream()
    {
        throw new UnsupportedOperationException("Cannot get a directory plugin as an InputStream");
    }
}
