package com.atlassian.plugin.event;

/**
 * Defines the event manager for use with internal Atlassian Plugins framework events.  How listeners are defined is up
 * to the implementation.  Implementations should allow listeners to somehow identify which events they would like to
 * listen for, then have the appropriate methods called if the event is the desired class or a subclass/implementation.
 * This means a listener could listen for an event of type java.lang.Object and should be notified for every event.
 *
 */
public interface PluginEventManager
{
    /**
     * Registers a listener object
     *
     * @param listener The listener instance.  Cannot be null.
     */
    void register(Object listener);

    /**
     * Unregisters a listener object
     *
     * @param listener The listener.  Cannot be null.
     */
    void unregister(Object listener);

    /**
     * Broadcasts an event to all applicable listeners
     * @param event The event object. Cannot be null.
     */
    void broadcast(Object event);
}
