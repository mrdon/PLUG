package com.atlassian.plugin.impl;

import com.atlassian.plugin.util.ClassLoaderUtils;

import java.io.InputStream;
import java.net.URL;

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
    {}

    public void setDeletable(final boolean deletable)
    {
        this.deletable = deletable;
    }

    public void setBundled(final boolean bundled)
    {
        bundledPlugin = bundled;
    }

    public <M> Class<M> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return ClassLoaderUtils.loadClass(clazz, callingClass);
    }

    public ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }

    public URL getResource(final String name)
    {
        return ClassLoaderUtils.getResource(name, getClass());
    }

    public InputStream getResourceAsStream(final String name)
    {
        return ClassLoaderUtils.getResourceAsStream(name, getClass());
    }
}
