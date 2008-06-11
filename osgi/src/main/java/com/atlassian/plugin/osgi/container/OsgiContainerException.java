package com.atlassian.plugin.osgi.container;

/**
 * Generic wrapper exception for any OSGi-related exceptions
 */
public class OsgiContainerException extends RuntimeException
{
    public OsgiContainerException()
    {
    }

    public OsgiContainerException(String s)
    {
        super(s);
    }

    public OsgiContainerException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public OsgiContainerException(Throwable throwable)
    {
        super(throwable);
    }
}
