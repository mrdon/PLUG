package com.atlassian.plugin.event.impl;

import static com.atlassian.plugin.util.Assertions.notNull;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.config.ListenerHandlersConfiguration;
import com.atlassian.event.internal.AsynchronousAbleEventDispatcher;
import com.atlassian.event.internal.EventExecutorFactoryImpl;
import com.atlassian.event.internal.EventPublisherImpl;
import com.atlassian.event.internal.EventThreadPoolConfigurationImpl;
import com.atlassian.event.spi.EventDispatcher;
import com.atlassian.event.spi.EventExecutorFactory;
import com.atlassian.event.spi.ListenerHandler;
import com.atlassian.plugin.event.NotificationException;
import com.atlassian.plugin.event.PluginEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple, synchronous event manager that uses one or more method selectors to determine event listeners.
 * <p>
 * The default method selectors are {@link MethodNameListenerMethodSelector} and 
 * {@link AnnotationListenerMethodSelector}.
 */
public class DefaultPluginEventManager implements PluginEventManager
{
    private final EventPublisher publisher;

    /**
     * Uses the supplied selectors to determine listener methods.
     * 
     * @param selectors used to determine which are listener methods
     */
    public DefaultPluginEventManager(final ListenerMethodSelector... selectors)
    {
        final ListenerHandlersConfiguration configuration = new ListenerHandlersConfiguration()
        {
            public List<ListenerHandler> getListenerHandlers()
            {
                final List<ListenerHandler> handlers = new ArrayList<ListenerHandler>(selectors.length);
                for (final ListenerMethodSelector selector : selectors)
                {
                    handlers.add(new MethodSelectorListenerHandler(selector));
                }
                return handlers;
            }
        };

        final EventExecutorFactory executorFactory = new EventExecutorFactoryImpl(new EventThreadPoolConfigurationImpl());
        final EventDispatcher eventDispatcher = new AsynchronousAbleEventDispatcher(executorFactory);
        publisher = new EventPublisherImpl(eventDispatcher, configuration);
    }

    public DefaultPluginEventManager()
    {
        this(defaultMethodSelectors());
    }

    /**
     * Delegate all event publication to the supplied {@code EventPublisher}.
     */
    public DefaultPluginEventManager(final EventPublisher publisher)
    {
        this.publisher = notNull("publisher", publisher);
    }

    public void register(final Object listener)
    {
        publisher.register(notNull("listener", listener));
    }

    public void unregister(final Object listener)
    {
        publisher.unregister(notNull("listener", listener));
    }

    public void broadcast(final Object event) throws NotificationException
    {
        notNull("event", event);
        try
        {
            publisher.publish(event);
        }
        catch (final RuntimeException e)
        {
            throw new NotificationException(e);
        }
    }

    static ListenerMethodSelector[] defaultMethodSelectors()
    {
        final ListenerMethodSelector methodNames = new MethodNameListenerMethodSelector();
        final ListenerMethodSelector pluginEvent = new AnnotationListenerMethodSelector();
        final ListenerMethodSelector eventListener = new AnnotationListenerMethodSelector(EventListener.class);
        return new ListenerMethodSelector[] { methodNames, pluginEvent, eventListener };
    }
}
