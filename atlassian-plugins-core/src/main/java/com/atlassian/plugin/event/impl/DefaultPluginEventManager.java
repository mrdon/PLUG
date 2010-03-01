package com.atlassian.plugin.event.impl;

import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.NotificationException;
import com.atlassian.plugin.util.ClassUtils;
import com.atlassian.plugin.util.collect.Function;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.internal.EventPublisherImpl;
import com.atlassian.event.internal.AsynchronousAbleEventDispatcher;
import com.atlassian.event.internal.EventExecutorFactoryImpl;
import com.atlassian.event.internal.EventThreadPoolConfigurationImpl;
import com.atlassian.event.internal.AnnotatedMethodsListenerHandler;
import com.atlassian.event.config.ListenerHandlersConfiguration;
import com.atlassian.event.config.EventThreadPoolConfiguration;
import com.atlassian.event.spi.ListenerHandler;
import com.atlassian.event.spi.EventDispatcher;
import com.atlassian.event.spi.EventExecutorFactory;
import com.google.common.collect.Collections2;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple, synchronous event manager that uses one or more method selectors to determine event listeners.  The default
 * method selectors are {@link MethodNameListenerMethodSelector} and {@link AnnotationListenerMethodSelector}.
 */
public class DefaultPluginEventManager implements PluginEventManager
{
    private final EventPublisher publisher;

    /**
     * Constructor that looks for an arbitrary selectors
     * @param selectors List of selectors that determine which are listener methods
     */
    public DefaultPluginEventManager(final ListenerMethodSelector[] selectors)
    {
        ListenerHandlersConfiguration configuration = new ListenerHandlersConfiguration()
        {
            public List<ListenerHandler> getListenerHandlers()
            {
                List<ListenerHandler> handlers = new ArrayList<ListenerHandler>(selectors.length);
                for(ListenerMethodSelector selector : selectors)
                {
                    handlers.add(new MethodSelectorListenerHandler(selector));
                }
                return handlers;
            }
        };

        EventThreadPoolConfiguration threadPoolConfiguration = new EventThreadPoolConfigurationImpl();
        EventExecutorFactory factory = new EventExecutorFactoryImpl(threadPoolConfiguration);
        EventDispatcher dispatcher = new AsynchronousAbleEventDispatcher(factory);
        publisher = new EventPublisherImpl(dispatcher, configuration);
    }

    public DefaultPluginEventManager()
    {
        this(new ListenerMethodSelector[] {new MethodNameListenerMethodSelector(), new AnnotationListenerMethodSelector(), new AnnotationListenerMethodSelector(EventListener.class)});
    }

    /**
     * Default constructor that delegates all event publication to an {@code EventPublisher}
     */
    public DefaultPluginEventManager(EventPublisher publisher)
    {
        this.publisher = publisher;
    }

    public void register(Object listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        publisher.register(listener);
    }

    public void unregister(Object listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        publisher.unregister(listener);
    }

    public void broadcast(Object event) throws NotificationException
    {
        publisher.publish(event);
    }
}
