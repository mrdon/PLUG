package com.atlassian.plugin.refimpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.util.tracker.ServiceTracker;

import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;

/**
 * Similar to the SimpleConstructorHostContainer, except it also scans the osgi container for registered components.
 */
class RefimplHostContainer implements HostContainer
{
    private final Map<Class<?>, Object> context;
    private final OsgiContainerManager osgiContainer;

    public RefimplHostContainer(Map<Class<?>, Object> context, OsgiContainerManager osgiContainer)
    {
        this.context = new ConcurrentHashMap<Class<?>, Object>(context);
        context.put(HostContainer.class, this);
        
        this.osgiContainer = osgiContainer;
    }

    /**
     * Creates a class instance, performing dependency injection using the initialised context map
     *
     * @param moduleClass The target object class
     * @return The instance
     * @throws IllegalArgumentException Wraps any exceptions thrown during the constructor call
     */
    public <T> T create(final Class<T> moduleClass) throws IllegalArgumentException
    {
        for (final Constructor<T> constructor : findConstructorsLargestFirst(moduleClass))
        {
            final List<Object> params = new ArrayList<Object>();
            for (final Class<?> paramType : constructor.getParameterTypes())
            {
                final ServiceTracker tracker = osgiContainer.getServiceTracker(paramType.getName());
                if (tracker == null)
                {
                    continue;
                }
                tracker.open();
                Object serviceProxy = Proxy.newProxyInstance(paramType.getClassLoader(), new Class<?>[] { paramType }, new InvocationHandler() 
                {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                    {
                        return method.invoke(tracker.getService(), args);
                    }
                });
                params.add(serviceProxy);
            }
            if (constructor.getParameterTypes().length != params.size())
            {
                continue;
            }

            try
            {
                return constructor.newInstance(params.toArray());
            }
            catch (final InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch (final IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch (final InvocationTargetException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        throw new IllegalArgumentException("Unable to match any constructor for class " + moduleClass);
    }

    /**
     * Gets a class instance of out of the context map
     *
     * @param moduleClass The object class
     * @return The object instance.  May be null.
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(final Class<T> moduleClass)
    {
        return (T) context.get(moduleClass);
    }

    @SuppressWarnings("unchecked")
    private <T> Collection<Constructor<T>> findConstructorsLargestFirst(final Class<T> moduleClass)
    {
        final Set<Constructor<T>> constructors = new TreeSet<Constructor<T>>(new Comparator<Constructor<T>>()
        {
            public int compare(final Constructor<T> first, final Constructor<T> second)
            {
                // @TODO this only sorts via largest, and therefore it also causes any of the same length to get dropped from the set, see TreeSet for more details
                return Integer.valueOf(second.getParameterTypes().length).compareTo(first.getParameterTypes().length);
            }
        });
        for (final Constructor<?> constructor : moduleClass.getConstructors())
        {
            constructors.add((Constructor<T>) constructor);
        }
        return constructors;
    }
}
