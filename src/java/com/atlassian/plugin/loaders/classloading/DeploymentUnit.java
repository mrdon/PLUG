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

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof DeploymentUnit)) return false;

        final DeploymentUnit deploymentUnit = (DeploymentUnit) o;

        if (!path.equals(deploymentUnit.path)) return false;

        return true;
    }

    public int hashCode()
    {
        return path.hashCode();
    }

    public String toString()
    {
        return "Unit: " + path.toString() + " (" + lastModified + ")";
    }
}
