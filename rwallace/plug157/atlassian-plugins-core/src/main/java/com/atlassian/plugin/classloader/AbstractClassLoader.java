package com.atlassian.plugin.classloader;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Collections;

/**
 * An abstract class loader to show what you need to implement.
 */
abstract class AbstractClassLoader extends ClassLoader
{
    protected AbstractClassLoader(ClassLoader parent)
    {
        super(parent);
    }

    protected AbstractClassLoader()
    {
        super();
    }

    protected abstract URL findResource(String name);

    protected abstract Class findClass(String className) throws ClassNotFoundException;

    /**
     * The default implementation returns a "singleton" enumeration over the result
     * of {@link #findResource(String)}.
     *
     * @param name the name of the resource
     * @return an enumeration over all matching resources
     * @throws IOException This implementation will not throw this exception
     */
    protected Enumeration<URL> findResources(String name) throws IOException
    {
        final URL url = this.findResource(name);
        return url != null ? Collections.enumeration(Collections.singleton(url)) : null;
    }
}