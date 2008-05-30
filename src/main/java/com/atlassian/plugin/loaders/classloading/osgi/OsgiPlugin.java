package com.atlassian.plugin.loaders.classloading.osgi;

import com.atlassian.plugin.impl.AbstractPlugin;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

import org.osgi.framework.Bundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: May 21, 2008
 * Time: 4:02:18 PM
 * This isn't the template you are looking for. It can go about its business.
 */
public class OsgiPlugin extends AbstractPlugin
{
    private Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable;
    private boolean bundled;

    public OsgiPlugin(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz, callingClass);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(String name)
    {
        return BundleClassLoaderAccessor.getResource(bundle, name);
    }

    public InputStream getResourceAsStream(String name)
    {
        return BundleClassLoaderAccessor.getResourceAsStream(bundle, name);
    }

    public ClassLoader getClassLoader()
    {
        return BundleClassLoaderAccessor.getClassLoader(bundle);
    }

    /**
     * This plugin is dynamically loaded, so returns true.
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
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
        // Do nothing
    }

}
