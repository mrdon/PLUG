package com.atlassian.plugin;

import org.apache.commons.lang.exception.NestableException;

public class PluginParseException extends NestableException
{
    public PluginParseException()
    {
        super();
    }

    public PluginParseException(String s)
    {
        super(s);
    }

    public PluginParseException(Throwable throwable)
    {
        super(throwable);
    }

    public PluginParseException(String s, Throwable throwable)
    {
        super(s, throwable);
    }
}
