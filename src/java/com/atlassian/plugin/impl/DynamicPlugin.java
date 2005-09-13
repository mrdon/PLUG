package com.atlassian.plugin.impl;

import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.InputStream;

public class DynamicPlugin extends StaticPlugin
{
    private DeploymentUnit deploymentUnit;
    private ClassLoader loader;

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

    /**
     * This plugin is dynamically loaded, so returns true.
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
    }

}
