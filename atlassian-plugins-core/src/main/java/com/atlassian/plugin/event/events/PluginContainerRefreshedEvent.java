package com.atlassian.plugin.event.events;

/**
 * Event for when the container a plugin is installed into has been refreshed
 *
 * @param <T> The container type
 * @since 2.2.0
 */
public class PluginContainerRefreshedEvent<T>
{
    private final T container;
    private final String key;

    public PluginContainerRefreshedEvent(T container, String key)
    {
        this.container = container;
        this.key = key;
    }

    public T getContainer()
    {
        return container;
    }

    public String getPluginKey()
    {
        return key;
    }
}
