package com.atlassian.plugin.artifact;

import org.apache.commons.lang.Validate;

import java.io.*;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;

public abstract class AbstractFilePluginArtifact implements PluginArtifact, Comparable<PluginArtifact>
{
    private final File file;
    private long lastModifiedAtTimeOfDeployment;

    public AbstractFilePluginArtifact(File file)
    {
        this(file, file.lastModified());
    }

    public AbstractFilePluginArtifact(File file, long lastModified)
    {
        Validate.notNull(file);
        this.lastModifiedAtTimeOfDeployment = lastModified;
        this.file = file;
    }

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public abstract InputStream getResourceAsStream(String fileName) throws PluginParseException;

    public String getName()
    {
        return file.getName();
    }

    public File getFile()
    {
        return file;
    }

    public long lastModified()
	{
		return lastModifiedAtTimeOfDeployment;
	}

    public int compareTo(PluginArtifact target)
    {
        int result = file.compareTo(target.getFile());
        if (result == 0)
            result = (lastModifiedAtTimeOfDeployment > target.lastModified() ? 1 :
                    lastModifiedAtTimeOfDeployment < target.lastModified() ? -1 : 0);
        return result;
    }

    public boolean equals(Object o)
    {
        if (o instanceof PluginArtifact)
        {
            PluginArtifact pluginArtifact = (PluginArtifact)o;
            if (!file.equals(pluginArtifact.getFile())) return false;
            if (lastModifiedAtTimeOfDeployment != pluginArtifact.lastModified()) return false;
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return a buffered file input stream of the file on disk. This input stream
     * is not resettable.
     */
    public InputStream getInputStream()
    {
        try
        {
            return new BufferedInputStream(new FileInputStream(file));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("Could not open plugin artifact file for reading: " + file, e);
        }
    }

    public int hashCode()
    {
        int result;
        result = file.hashCode();
        result = 31 * result + (int) (lastModifiedAtTimeOfDeployment ^ (lastModifiedAtTimeOfDeployment >>> 32));
        return result;
    }

    public String toString()
    {
        return "Plugin: " + file.toString() + " (" + lastModifiedAtTimeOfDeployment + ")";
    }
}
