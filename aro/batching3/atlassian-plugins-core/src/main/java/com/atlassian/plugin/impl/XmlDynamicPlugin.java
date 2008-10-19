package com.atlassian.plugin.impl;

import com.atlassian.plugin.util.ClassLoaderUtils;

import java.net.URL;
import java.io.InputStream;

/**
 * A dynamic XML plugin that consists of the Atlassian plugin descriptor
 *
 * @since 2.1.0
 */
public class XmlDynamicPlugin extends AbstractPlugin implements DynamicPlugin
{
    private boolean bundledPlugin;
    private boolean deletable;


    public boolean isBundledPlugin()
    {
        return bundledPlugin;
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public boolean isDeleteable()
    {
        return deletable;
    }

    public boolean isDynamicallyLoaded()
    {
        return true;
    }

    public void close()
    {
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public void setBundled(boolean bundled)
    {
        this.bundledPlugin = bundled;
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
}
