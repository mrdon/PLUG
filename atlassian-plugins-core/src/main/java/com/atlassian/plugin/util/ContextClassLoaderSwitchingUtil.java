package com.atlassian.plugin.util;

/**
 * Utility to run Runnable implementations in a different ClassLoader.
 *
 * @since 2.5.0
 */
public class ContextClassLoaderSwitchingUtil
{
    /**
     * Executes the provided {@link Runnable} implementation in the specified {@link ClassLoader}.
     * <p/>
     * Utilises the {@link com.atlassian.plugin.util.ClassLoaderStack#push} method to save the old {@link ClassLoader} and 
     * set the one specified as <code>newClassLoader</code>. {@link com.atlassian.plugin.util.ClassLoaderStack#pop} is
     * called in a finally block to ensure the {@link ClassLoader} is set back to the original one.
     *
     * @param newClassLoader The {@link ClassLoader} to run the specified {@link Runnable} in.
     * @param runnable The implementation to be run in the specified {@link ClassLoader}
     */
    public static void runInContext(ClassLoader newClassLoader, Runnable runnable)
    {
        ClassLoaderStack.push(newClassLoader);
        try
        {
            runnable.run();
        }
        finally
        {
            ClassLoaderStack.pop();
        }
    }
}
