package com.atlassian.plugin.event;

import com.atlassian.plugin.PluginException;

/**
 * This is used to wrap an exception thrown by a Plugin Event Listener.
 *
 * @since v4.0
 */
public class NotificationException extends PluginException
{
    public NotificationException(final Throwable cause)
    {
        super(cause);
    }
}
