package com.atlassian.plugin.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.URL;

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
        if (delegateClassLoader != null)
        {
            return delegateClassLoader.loadClass(name);
        }
        else
        {
            throw new ClassNotFoundException("Couldn't find class [" + name + "], no delegateClassLoader to delegate to.");
        }
    }

    public URL getResource(String name)
    {
        return delegateClassLoader != null ? delegateClassLoader.getResource(name) : null;
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
