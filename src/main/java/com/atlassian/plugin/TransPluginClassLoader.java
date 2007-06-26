package com.atlassian.plugin;

import com.atlassian.plugin.impl.DynamicPlugin;

import java.net.URL;
import java.io.InputStream;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class loader will search the class loaders of all enabled dynamic plugins if it cannot find a class in the
 * default class loader.
 */
public class TransPluginClassLoader extends ClassLoader
{
    private PluginManager pluginManager;

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
            o = eachClassLoader(TransPluginClassLoader.class.getClassLoader());
            if (o != null)
            {
                return o;
            }
            Collection plugins = pluginManager.getPlugins();
            for (Iterator i = plugins.iterator(); i.hasNext();)
            {
                Plugin p = (Plugin) i.next();
                if (p instanceof DynamicPlugin)
                {
                    DynamicPlugin dp = (DynamicPlugin) p;
                    o = eachClassLoader(dp.getClassLoader());
                    if (o != null)
                    {
                        return o;
                    }
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


    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }
}
