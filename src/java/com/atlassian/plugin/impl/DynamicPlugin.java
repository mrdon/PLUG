package com.atlassian.plugin.impl;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.InputStream;

public class DynamicPlugin extends StaticPlugin
{
    private DeploymentUnit deploymentUnit;
    private ClassLoader loader;
    private boolean deletable = true;
    private boolean bundled = false;

    public DynamicPlugin(DeploymentUnit deploymentUnit, ClassLoader loader)
    {
        super();
        this.deploymentUnit = deploymentUnit;
        this.loader = loader;
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return loader.loadClass(clazz);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public InputStream getResourceAsStream(String name)
    {
        return loader.getResourceAsStream(name);
    }

    public ClassLoader getClassLoader()
    {
        return loader;
    }

    /**
     * This plugin is dynamically loaded, so returns true.
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
    }

    public DeploymentUnit getDeploymentUnit()
    {
        return deploymentUnit;
    }

    public boolean isDeleteable()
    {
        return deletable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public boolean isBundledPlugin()
    {
        return bundled;
    }

    public void setBundled(boolean bundled)
    {
        this.bundled = bundled;
    }
}
