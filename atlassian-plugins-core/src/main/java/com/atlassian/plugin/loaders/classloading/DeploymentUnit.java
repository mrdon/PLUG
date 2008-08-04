package com.atlassian.plugin.loaders.classloading;

import java.io.File;

public class DeploymentUnit implements Comparable
{
	private final File path;
    private long lastModifiedAtTimeOfDeployment;

    public DeploymentUnit(File path)
	{
        if (path == null)
        {
            throw new IllegalArgumentException("File should not be null!");
        }
        this.path = path;
        this.lastModifiedAtTimeOfDeployment = path.lastModified();
    }

	public long lastModified()
	{
		return lastModifiedAtTimeOfDeployment;
	}

	public File getPath()
	{
		return path;
	}

    public int compareTo(Object o)
    {
        if (!(o instanceof DeploymentUnit))
            return 1;

        DeploymentUnit target = (DeploymentUnit) o;

        int result = path.compareTo(target.getPath());
        if (result == 0)
            result = (lastModifiedAtTimeOfDeployment > target.lastModified() ? 1 :
                    lastModifiedAtTimeOfDeployment < target.lastModified() ? -1 : 0);
        return result;
    }



    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof DeploymentUnit)) return false;

        final DeploymentUnit deploymentUnit = (DeploymentUnit) o;

        if (!path.equals(deploymentUnit.path)) return false;
        if (lastModifiedAtTimeOfDeployment != deploymentUnit.lastModifiedAtTimeOfDeployment) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = path.hashCode();
        result = 31 * result + (int) (lastModifiedAtTimeOfDeployment ^ (lastModifiedAtTimeOfDeployment >>> 32));
        return result;
    }

    public String toString()
    {
        return "Unit: " + path.toString() + " (" + lastModifiedAtTimeOfDeployment + ")";
    }
}
