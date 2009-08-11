package com.atlassian.plugin.event.impl;

import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.NotificationException;
import com.atlassian.plugin.util.ClassUtils;
import com.atlassian.plugin.util.collect.Function;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple, synchronous event manager that uses one or more method selectors to determine event listeners.  The default
 * method selectors are {@link MethodNameListenerMethodSelector} and {@link AnnotationListenerMethodSelector}.
 */
public class DefaultPluginEventManager implements PluginEventManager
{
    private static final Log log = LogFactory.getLog(DefaultPluginEventManager.class);

    private final EventsToListener eventsToListener = new EventsToListener();
    private final ListenerMethodSelector[] listenerMethodSelectors;

    /**
     * Default constructor that looks for methods named "channel" and the @PluginEventListener annotations
     */
    public DefaultPluginEventManager()
    {
        this(new ListenerMethodSelector[] { new MethodNameListenerMethodSelector(), new AnnotationListenerMethodSelector() });
    }

    /**
     * Constructor that looks for an arbitrary selectors
     * @param selectors List of selectors that determine which are listener methods
     */
    public DefaultPluginEventManager(final ListenerMethodSelector[] selectors)
    {
        listenerMethodSelectors = selectors;
    }

    public void broadcast(final Object event) throws NotificationException
    {
        Validate.notNull(event, "The event to broadcast must not be null");
        final Set<Listener> calledListeners = new HashSet<Listener>();
        NotificationException notificationException = null;
        for (final Class<?> type : ClassUtils.findAllTypes(event.getClass()))
        {
            final Set<Listener> registrations = eventsToListener.get(type);
            if (registrations != null)
            {
                for (final Listener reg : registrations)
                {
                    if (calledListeners.contains(reg))
                    {
                        continue;
                    }
                    calledListeners.add(reg);
                    try
                    {
                        reg.notify(event);
                    }
                    catch (NotificationException ex)
                    {
                        // remember this exception, but notify all other listeners before we throw it.
                        notificationException = ex;
                    }
                }
            }
        }

        if (notificationException != null)
        {
            // One of the listeners threw an exception - we rethrow it now that we have notified other listeners.
            throw notificationException;
        }
    }

    /**
     * Registers a listener by scanning the object for all listener methods
     *
     * @param listener The listener object
     * @throws IllegalArgumentException If the listener is null, contains a listener method with 0 or 2 or more
     * arguments, or contains no listener methods
     */
    public void register(final Object listener) throws IllegalArgumentException
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        final AtomicBoolean listenerFound = new AtomicBoolean(false);
        forEveryListenerMethod(listener, new ListenerMethodHandler()
        {
            public void handle(final Object listener, final Method m)
            {
                if (m.getParameterTypes().length != 1)
                {
                    throw new IllegalArgumentException("Listener methods must only have one argument");
                }
                final Set<Listener> listeners = eventsToListener.get(m.getParameterTypes()[0]);
                listeners.add(new Listener(listener, m));
                listenerFound.set(true);
            }
        });
        if (!listenerFound.get())
        {
            throw new IllegalArgumentException(
                "At least one listener method must be specified.  Most likely, a listener " + "method is missing the @PluginEventListener annotation.");
        }
    }

    /**
     * Unregisters the listener
     * @param listener The listener
     */
    public void unregister(final Object listener)
    {
        forEveryListenerMethod(listener, new ListenerMethodHandler()
        {
            public void handle(final Object listener, final Method m)
            {
                final Set<Listener> listeners = eventsToListener.get(m.getParameterTypes()[0]);
                listeners.remove(new Listener(listener, m));
            }
        });
    }

    /**
     * Walks an object for every listener method and calls the handler
     * @param listener The listener object
     * @param handler The handler
     */
    void forEveryListenerMethod(final Object listener, final ListenerMethodHandler handler)
    {
        final Method[] methods = listener.getClass().getMethods();
        for (final Method m : methods)
        {
            for (final ListenerMethodSelector selector : listenerMethodSelectors)
            {
                if (selector.isListenerMethod(m))
                {
                    handler.handle(listener, m);
                }
            }
        }
    }

    /**
     * Records a registration of a listener method
     */
    /**
     * Simple fake closure for logic that needs to execute for every listener method on an object
     */
    private static interface ListenerMethodHandler
    {
        void handle(Object listener, Method m);
    }

    private static class Listener
    {

        public final Object listener;

        public final Method method;

        public Listener(final Object listener, final Method method)
        {
            Validate.notNull(listener);
            Validate.notNull(method);
            this.listener = listener;
            this.method = method;
        }

        public void notify(final Object event) throws NotificationException
        {
            Validate.notNull(event);
            try
            {
                method.invoke(listener, event);
            }
            catch (final IllegalAccessException e)
            {
                log.error("Unable to access listener method: " + method, e);
            }
            catch (final InvocationTargetException e)
            {
                // InvocationTargetException wraps an error thrown by the Event Listener. We re-wrap it in our NotificationException.
                throw new NotificationException(e.getCause());
            }
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass()))
            {
                return false;
            }

            final Listener that = (Listener) o;

            if (!listener.equals(that.listener))
            {
                return false;
            }
            if (!method.equals(that.method))
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result;
            result = listener.hashCode();
            result = 31 * result + method.hashCode();
            return result;
        }
    }

    static class EventsToListener implements Function<Class<?>, Set<Listener>>
    {
        private final ConcurrentMap<Class<?>, Set<Listener>> map = CopyOnWriteMap.newHashMap();

        public Set<Listener> get(final Class<?> input)
        {
            Set<Listener> result = map.get(input);
            while (result == null)
            {
                map.putIfAbsent(input, new CopyOnWriteArraySet<Listener>());
                result = map.get(input);
            }
            return result;
        }
    }
}
