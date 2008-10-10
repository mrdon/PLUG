package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class DirectoryPluginArtifact extends AbstractFilePluginArtifact
{
    public DirectoryPluginArtifact(File directory)
    {
        super(directory);
        if (!directory.isDirectory())
            throw new IllegalArgumentException(getClass().getSimpleName() + " called with non-directory " + directory);
    }

    public Iterable<String> getResourceNames() throws IOException
    {
        Collection<String> directoryContents = new ArrayList<String>();
        appendContents("", getFile(), directoryContents);
        return directoryContents;
    }

    private void appendContents(String prefix, File directory, Collection<String> collection)
    {
        assert directory.isDirectory();
        for (File childFile : directory.listFiles())
        {
            final String relativePath = prefix + childFile.getName();
            collection.add(relativePath);
            if (childFile.isDirectory())
            {
                appendContents(relativePath + "/", childFile, collection);
            }
        }
    }

    public URL getResource(String fileName)
    {
        try
        {
            return getResourceFile(fileName).toURL();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public InputStream getResourceAsStream(String fileName) throws PluginParseException
    {
        try
        {
            return new BufferedInputStream(new FileInputStream(getResourceFile(fileName)));
        }
        catch (IOException e)
        {
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin directory [" + getFile() + "]", e);
        }
    }

    private File getResourceFile(String fileName)
    {
        Validate.notNull(fileName, "The file name must not be null");
        File resource = new File(getFile(), fileName);
        if (!resource.exists())
        {
            throw new PluginParseException("File " + fileName + " not found in plugin directory [" + getFile() + "]");
        }
        return resource;
    }

    public InputStream getInputStream()
    {
        throw new UnsupportedOperationException("Cannot get a directory plugin as an InputStream");
    }
}
