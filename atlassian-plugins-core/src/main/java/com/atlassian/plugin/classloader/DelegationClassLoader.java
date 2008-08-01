package com.atlassian.plugin.classloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

import java.io.InputStream;
import java.net.URL;

/**
 * A class loader that delegates to another class loader.
 */
public class DelegationClassLoader extends ClassLoader
{
    private static final Log log = LogFactory.getLog(DelegationClassLoader.class);

    private ClassLoader delegateClassLoader = DelegationClassLoader.class.getClassLoader();

    public void setDelegateClassLoader(ClassLoader delegateClassLoader)
    {
        Validate.notNull(delegateClassLoader, "Can't set the delegation target to null");
        if (log.isDebugEnabled())
        {
            log.debug("Update class loader delegation from [" + this.delegateClassLoader +
                    "] to [" + delegateClassLoader + "]");
        }
        this.delegateClassLoader = delegateClassLoader;
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        return delegateClassLoader.loadClass(name);
    }

    public URL getResource(String name)
    {
        return delegateClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(String name)
    {
        return delegateClassLoader.getResourceAsStream(name);
    }

    public synchronized void setDefaultAssertionStatus(boolean enabled)
    {
        delegateClassLoader.setDefaultAssertionStatus(enabled);
    }

    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled)
    {
        delegateClassLoader.setPackageAssertionStatus(packageName, enabled);
    }

    public synchronized void setClassAssertionStatus(String className, boolean enabled)
    {
        delegateClassLoader.setClassAssertionStatus(className, enabled);
    }

    public synchronized void clearAssertionStatus()
    {
        delegateClassLoader.clearAssertionStatus();
    }
}
