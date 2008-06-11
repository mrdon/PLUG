package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.StateAware;

import java.net.URL;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.
 */
public class OsgiPlugin extends AbstractPlugin implements StateAware
{
    private Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable = true;
    private boolean bundled = false;

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

    public boolean isEnabled()
    {
        return Bundle.ACTIVE == bundle.getState();
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled) {
            enabled();
        }
        else {
            disabled();
        }
    }

    public void enabled()
    {
        try
        {
            bundle.start();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot start plugin: "+getKey());
        }
    }

    public void disabled()
    {
        try
        {
            bundle.stop();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot stop plugin: "+getKey());
        }
    }

    public void close()
    {
        try
        {
            bundle.uninstall();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot uninstall bundle " + bundle.getSymbolicName());
        }
    }

}
