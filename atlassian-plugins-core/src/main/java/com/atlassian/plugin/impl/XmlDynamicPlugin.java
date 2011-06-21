package com.atlassian.plugin.impl;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginArtifactBackedPlugin;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.io.InputStream;
import java.net.URL;

/**
 * A dynamic XML plugin that consists of the Atlassian plugin descriptor
 *
 * @since 2.1.0
 */
public class XmlDynamicPlugin extends AbstractPlugin implements PluginArtifactBackedPlugin
{
    private final PluginArtifact pluginArtifact;

    public XmlDynamicPlugin(PluginArtifact pluginArtifact)
    {
        this.pluginArtifact = pluginArtifact;
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public boolean isDeleteable()
    {
        return true;
    }

    public boolean isDynamicallyLoaded()
    {
        return true;
    }

    public void close()
    {}

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

    public PluginArtifact getPluginArtifact()
    {
        return pluginArtifact;
    }
}
