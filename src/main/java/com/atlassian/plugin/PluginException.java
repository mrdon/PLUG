package com.atlassian.plugin;

/**
 * Generic plugin exception.
 */
import org.apache.commons.lang.exception.NestableException;

public class PluginException extends NestableException
{
    ///CLOVER:OFF
    public PluginException()
    {
        super();
    }

    public PluginException(String s)
    {
        super(s);
    }

    public PluginException(Throwable throwable)
    {
        super(throwable);
    }

    public PluginException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

}
