package com.atlassian.plugin.osgi.loader.transform;

public class PluginTransformationException extends RuntimeException
{
    public PluginTransformationException()
    {
    }

    public PluginTransformationException(String s)
    {
        super(s);
    }

    public PluginTransformationException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public PluginTransformationException(Throwable throwable)
    {
        super(throwable);
    }
}
