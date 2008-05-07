package com.atlassian.plugin.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;

/**
 * A class loader that delegates to another class loader.
 */
public class DelegationClassLoader extends ClassLoader
{
    private static final Log log = LogFactory.getLog(DelegationClassLoader.class);

    private ClassLoader delegateClassLoader;

    public void setDelegateClassLoader(ClassLoader delegateClassLoader)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Update class loader delegation from [" + this.delegateClassLoader +
                    "] to [" + delegateClassLoader + "]");
        }
        this.delegateClassLoader = delegateClassLoader;
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        return delegateClassLoader != null ? delegateClassLoader.loadClass(name) : null;
    }

    public URL getResource(String name)
    {
        return delegateClassLoader != null ? delegateClassLoader.getResource(name) : null;
    }

    public Enumeration getResources(String name) throws IOException
    {
        return delegateClassLoader != null ? delegateClassLoader.getResources(name) : null;
    }

    public InputStream getResourceAsStream(String name)
    {
        return delegateClassLoader != null ? delegateClassLoader.getResourceAsStream(name) : null;
    }

    public synchronized void setDefaultAssertionStatus(boolean enabled)
    {
        if (delegateClassLoader != null)
        {
            delegateClassLoader.setDefaultAssertionStatus(enabled);
        }
    }

    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled)
    {
        if (delegateClassLoader != null)
        {
            delegateClassLoader.setPackageAssertionStatus(packageName, enabled);
        }
    }

    public synchronized void setClassAssertionStatus(String className, boolean enabled)
    {
        if (delegateClassLoader != null)
        {
            delegateClassLoader.setClassAssertionStatus(className, enabled);
        }
    }

    public synchronized void clearAssertionStatus()
    {
        if (delegateClassLoader != null)
        {
            delegateClassLoader.clearAssertionStatus();
        }
    }
}
