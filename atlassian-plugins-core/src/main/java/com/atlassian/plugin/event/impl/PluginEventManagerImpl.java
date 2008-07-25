package com.atlassian.plugin.event.impl;

import com.atlassian.plugin.event.PluginEventManager;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.Factory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

/**
 * Simple, synchronous event manager that uses one or more method selectors to determine event listeners.  The default
 * method selector is {@link MethodNameListenerMethodSelector}.
 */
public class PluginEventManagerImpl implements PluginEventManager
{
    private final Map/*<Class,Set<Listener>>*/ eventsToListener;
    private static final Log log = LogFactory.getLog(PluginEventManagerImpl.class);
    private final ListenerMethodSelector[] listenerMethodSelectors;

    /**
     * Default constructor that looks for methods named "channel"
     */
    public PluginEventManagerImpl()
    {
        this(new ListenerMethodSelector[]{new MethodNameListenerMethodSelector()});
    }

    /**
     * Constructor that looks for an arbitrary selectors
     * @param selectors List of selectors that determine which are listener methods
     */
    public PluginEventManagerImpl(ListenerMethodSelector[] selectors)
    {
        this.listenerMethodSelectors = selectors;
        eventsToListener = LazyMap.decorate(new HashMap(), new Factory() {
            public Object create() { return new HashSet(); }
        });
    }

    /**
     * Broadcasts an event to all listeners synchronously, logging all exceptions as an ERROR.
     *
     * @param event The event object
     */
    public synchronized void broadcast(Object event)
    {
        Validate.notNull(event, "The event to broadcast must not be null");
        final Set/*<Method>*/ calledListeners = new HashSet/*<Method>*/();
        Set/*<Class>*/ types = new HashSet/*<Class>*/();
        findAllTypes(event.getClass(), types);
        for (Iterator clsitr = types.iterator(); clsitr.hasNext(); )
        {
            Class type = (Class) clsitr.next();
            Set registrations = (Set) eventsToListener.get(type);
            for (Iterator i = registrations.iterator(); i.hasNext(); )
            {
                Listener reg = (Listener) i.next();
                if (calledListeners.contains(reg))
                    continue;
                calledListeners.add(reg);
                reg.notify(event);
            }
        }
    }

    /**
     * Registers a listener by scanning the object for all listener methods
     *
     * @param listener The listener object
     * @throws IllegalArgumentException If the listener is null or contains a listener method with 0 or 2 or more
     * arguments
     */
    public synchronized void register(Object listener) throws IllegalArgumentException
    {
        if (listener == null)
            throw new IllegalArgumentException("Listener cannot be null");

        forEveryListenerMethod(listener, new ListenerMethodHandler()
        {
            public void handle(Object listener, Method m)
            {
                if (m.getParameterTypes().length != 1)
                        throw new IllegalArgumentException("Listener methods must only have one argument");
                Set/*<Listener>*/ listeners = (Set) eventsToListener.get(m.getParameterTypes()[0]);
                listeners.add(new Listener(listener, m));
            }
        });
    }

    /**
     * Unregisters the listener
     * @param listener The listener
     */
    public synchronized void unregister(Object listener)
    {
        forEveryListenerMethod(listener, new ListenerMethodHandler()
        {
            public void handle(Object listener, Method m)
            {
                Set/*<Listener>*/ listeners = (Set) eventsToListener.get(m.getParameterTypes()[0]);
                listeners.remove(new Listener(listener, m));
            }
        });
    }

    /**
     * Walks an object for every listener method and calls the handler
     * @param listener The listener object
     * @param handler The handler
     */
    void forEveryListenerMethod(Object listener, ListenerMethodHandler handler)
    {
        Method[] methods = listener.getClass().getMethods();
        for (int x=0; x<methods.length; x++)
        {
            Method m = methods[x];
            for (int s = 0; s<listenerMethodSelectors.length; s++)
            {
                ListenerMethodSelector selector = listenerMethodSelectors[s];
                if (selector.isListenerMethod(m))
                {
                    handler.handle(listener, m);
                }
            }
        }
    }

    /**
     * Finds all super classes and interfaces for a given class
     * @param cls The class to scan
     * @param types The collected related classes found
     */
    void findAllTypes(Class cls, Set/*<Class>*/ types)
    {
        if (cls == null)
            return;

        types.add(cls);

        findAllTypes(cls.getSuperclass(), types);
        for (int x = 0; x<cls.getInterfaces().length; x++)
            findAllTypes(cls.getInterfaces()[x], types);
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

        public Listener(Object listener, Method method)
        {
            Validate.notNull(listener);
            Validate.notNull(method);
            this.listener = listener;
            this.method = method;
        }

        public void notify(Object event)
        {
            Validate.notNull(event);
            try
            {
                method.invoke(listener, new Object[]{event});
            }
            catch (IllegalAccessException e)
            {
                log.error("Unable to access listener method: "+method, e);
            }
            catch (InvocationTargetException e)
            {
                log.error("Exception calling listener method", e.getCause());
            }
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Listener that = (Listener) o;

            if (!listener.equals(that.listener)) return false;
            if (!method.equals(that.method)) return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = listener.hashCode();
            result = 31 * result + method.hashCode();
            return result;
        }
    }
}
