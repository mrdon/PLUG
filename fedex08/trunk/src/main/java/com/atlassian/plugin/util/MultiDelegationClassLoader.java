package com.atlassian.plugin.util;

import java.util.Iterator;
import java.util.List;
import java.net.URL;
import java.io.InputStream;

/**
 * A classloader that searches for classes in a number of delegate classloaders. Search order is
 * (1) the context classloader, (2) the classloader responsible for loading this class, (3) the
 * list of classloaders provided by the implementing class, in list order.
 */
public abstract class MultiDelegationClassLoader extends ClassLoader
{
    /**
     * Override to return the list of classloaders that this classloader should search to find classes. You
     * do not need to add the context classloader or MultiDelegationClassLoader's own classloader, as both will
     * be searched first anyway.
     *
     * @return the delegate classloaders to search for classes.
     */
    protected abstract List/*<ClassLoader>*/ getClassLoaders();

    /**
     * This class encapsulates our class loading strategy.
     */
    private abstract class Search
    {
        public abstract Object eachClassLoader(ClassLoader cl);

        public Object doSearch()
        {
            Object o = eachClassLoader(Thread.currentThread().getContextClassLoader());
            if (o != null)
            {
                return o;
            }

            o = eachClassLoader(MultiDelegationClassLoader.class.getClassLoader());
            if (o != null)
            {
                return o;
            }

            for (Iterator it = getClassLoaders().iterator(); it.hasNext();)
            {
                o = eachClassLoader((ClassLoader) it.next());
                if (o != null)
                {
                    return o;
                }
            }

            return null;
        }

    }

    public URL getResource(final String name)
    {
        return (URL)new Search() {
            public Object eachClassLoader(ClassLoader cl)
            {
                return cl.getResource(name);
            }
        }.doSearch();
    }

    public InputStream getResourceAsStream(final String name)
    {
        return (InputStream)new Search() {
            public Object eachClassLoader(ClassLoader cl)
            {
                return cl.getResourceAsStream(name);
            }
        }.doSearch();
    }

    public Class loadClass(final String name) throws ClassNotFoundException
    {
        Class c = (Class)new Search() {
            public Object eachClassLoader(ClassLoader cl)
            {
                try
                {
                    return cl.loadClass(name);
                }
                catch (ClassNotFoundException e)
                {
                    return null;
                }
            }
        }.doSearch();
        if (c == null)
        {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

}
