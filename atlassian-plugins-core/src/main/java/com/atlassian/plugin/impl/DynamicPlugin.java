package com.atlassian.plugin.impl;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.net.URL;
import java.io.InputStream;

public interface DynamicPlugin extends Plugin
{
    Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException;

    boolean isUninstallable();

    URL getResource(String name);

    InputStream getResourceAsStream(String name);

    ClassLoader getClassLoader();

    /**
     * This plugin is dynamically loaded, so returns true.
     *
     * @return true
     */
    boolean isDynamicallyLoaded();

    boolean isDeleteable();

    void setDeletable(boolean deletable);

    boolean isBundledPlugin();

    void setBundled(boolean bundled);

    void close();
}
