package com.atlassian.plugin.loaders.classloading;

import java.io.File;

public class DeploymentUnit implements Comparable
{
	File path;
	long lastModified;

	public DeploymentUnit(File path)
	{
		this.path = path;
		this.lastModified = path.lastModified();
	}

	public long lastModified()
	{
		return lastModified;
	}

	public File getPath()
	{
		return path;
	}

    public int compareTo(Object o)
    {
        if (!(o instanceof DeploymentUnit))
            return 1;

        return path.compareTo(((DeploymentUnit) o).getPath());
    }

    public String toString()
    {
        return "Unit: " + path.toString() + " (" + lastModified + ")";
    }
}
