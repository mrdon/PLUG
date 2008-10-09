package com.atlassian.plugin.impl;

import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.PluginArtifact;

import java.io.InputStream;
import java.net.URL;

/**
 * A dynamically loaded plugin is loaded through the plugin class loader.
 */
public class DefaultDynamicPlugin extends AbstractPlugin implements DynamicPlugin
{
    private final PluginArtifact pluginArtifact;
    private final PluginClassLoader loader;
    private boolean deletable = true;
    private boolean bundled = false;

    /**
     * @deprecated Since 2.1.0
     */
    public DefaultDynamicPlugin(DeploymentUnit pluginArtifact, PluginClassLoader loader)
    {
        this((PluginArtifact) pluginArtifact, loader);
    }
    public DefaultDynamicPlugin(PluginArtifact pluginArtifact, PluginClassLoader loader)
    {
        if (loader == null)
        {
            throw new IllegalArgumentException("PluginClassLoader must not be null");
        }
        this.pluginArtifact = pluginArtifact;
        this.loader = loader;
    }

    public Class<?> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
    {
        return loader.loadClass(clazz);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(String name)
    {
        return loader.getResource(name);
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
     *
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
    }

    /**
     * @deprecated Since 2.1.0, use {@link #getPluginArtifact()} instead
     * @return
     */
    public DeploymentUnit getDeploymentUnit()
    {
        return new DeploymentUnit(pluginArtifact);
    }

    /**
     * @return
     * @since 2.1.0
     */
    public PluginArtifact getPluginArtifact()
    {
        return pluginArtifact;
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

    public void close()
    {
        loader.close();
    }
}
