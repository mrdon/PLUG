package com.atlassian.plugin.event.events;

/**
 * Event thrown when the container a plugin is installed into either rejects the plugin or fails altogether
 *
 * @since 2.2.0
 */
public class PluginContainerFailedEvent<T>
{
    private final T container;
    private final String key;
    private final Throwable cause;

    public PluginContainerFailedEvent(T container, String key, Throwable cause)
    {
        this.container = container;
        this.key = key;
        this.cause = cause;
    }

    public T getContainer()
    {
        return container;
    }

    public String getPluginKey()
    {
        return key;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
