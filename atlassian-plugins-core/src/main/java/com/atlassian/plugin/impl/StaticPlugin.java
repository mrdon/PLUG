package com.atlassian.plugin.impl;

import java.io.InputStream;
import java.net.URL;

import com.atlassian.plugin.util.ClassLoaderUtils;

public class StaticPlugin extends AbstractPlugin
{
    /**
     * Static plugins loaded from the classpath can't be uninstalled.
     */
    public boolean isUninstallable()
    {
        return false;
    }

    public Class<?> loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return ClassLoaderUtils.loadClass(clazz, callingClass);
    }

    public ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }
    
    public URL getResource(String name)
    {
        return ClassLoaderUtils.getResource(name, getClass());
    }
    
    public InputStream getResourceAsStream(String name)
    {
        return ClassLoaderUtils.getResourceAsStream(name, getClass());
    }

    public boolean isDynamicallyLoaded()
    {
        return false;
    }

    public boolean isBundledPlugin()
    {
        return false;
    }

    public boolean isDeleteable()
    {
        return false;
    }


    public void close()
    {

    }
}

