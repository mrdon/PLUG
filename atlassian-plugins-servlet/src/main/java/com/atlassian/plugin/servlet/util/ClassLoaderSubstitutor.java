package com.atlassian.plugin.servlet.util;

import com.atlassian.plugin.Plugin;

/**
 * A class to make substituting the current threads class loader with another and later restoring it.
 * This might also be implemented as a stack, but for now the needs are to do simple substitution.
 */
public class ClassLoaderSubstitutor
{
    private static final ThreadLocal<ClassLoader> startingClassLoader = new ThreadLocal<ClassLoader>();
    
    public static void substituteThreadClassLoaderWithClassLoaderFrom(Plugin plugin)
    {
        startingClassLoader.set(Thread.currentThread().getContextClassLoader());
        if (plugin.isDynamicallyLoaded())
        {
            Thread.currentThread().setContextClassLoader(plugin.getClassLoader());
        }
    }
    
    public static void restoreThreadClassLoader()
    {
        Thread.currentThread().setContextClassLoader(startingClassLoader.get());
    }
}
