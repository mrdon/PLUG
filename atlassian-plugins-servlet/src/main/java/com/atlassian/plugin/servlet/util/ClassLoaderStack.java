package com.atlassian.plugin.servlet.util;

import java.util.LinkedList;
import java.util.List;

/**
 * This utility provides a thread local stack of {@link ClassLoader}s.  The current "top" of the stack is the 
 * threads current context class loader.  This can be used when implementing delegating plugin {@link Filter}s or 
 * {@link Servlet}s that need to set the {@link ClassLoader} to the {@link PluginClassLoader} the filter or servlet is
 * declared in.
 */
public class ClassLoaderStack
{
    private static final ThreadLocal<List<ClassLoader>> classLoaderStack = new ThreadLocal<List<ClassLoader>>();
    static {
        classLoaderStack.set(new LinkedList<ClassLoader>());
    }
    
    public static void push(ClassLoader loader)
    {
        classLoaderStack.get().add(0, Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
    }
    
    public static void pop()
    {
        Thread.currentThread().setContextClassLoader(classLoaderStack.get().remove(0));
    }
}
