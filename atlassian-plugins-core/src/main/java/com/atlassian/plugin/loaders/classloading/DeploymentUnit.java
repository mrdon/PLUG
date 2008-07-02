package com.atlassian.plugin.loaders.classloading;

import java.io.File;

public class DeploymentUnit implements Comparable
{
	private final File path;

	public DeploymentUnit(File path)
	{
        if (path == null)
        {
            throw new IllegalArgumentException("File should not be null!");
        }
        this.path = path;
	}

	public long lastModified()
	{
		return path.lastModified();
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
        return "Unit: " + path.toString() + " (" + path.lastModified() + ")";
    }
}
