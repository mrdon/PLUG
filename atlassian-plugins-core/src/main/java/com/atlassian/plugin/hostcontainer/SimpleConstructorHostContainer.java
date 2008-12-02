package com.atlassian.plugin.hostcontainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Constructs module instances, matching the constructor with the largest number of arguments first.  The objects to
 * pass to the constructor are retrieved from the passed map of classes and objects.  The classes are matched on an
 * exact class match.
 *
 * @since 2.2.0
 */
public class SimpleConstructorHostContainer implements HostContainer
{
    private final Map<Class<?>, Object> context;

    public SimpleConstructorHostContainer(final Map<Class<?>, Object> context)
    {
        final Map<Class<?>, Object> tmp = new HashMap<Class<?>, Object>(context);
        tmp.put(HostContainer.class, this);
        this.context = Collections.unmodifiableMap(tmp);
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
                if (context.containsKey(paramType))
                {
                    params.add(context.get(paramType));
                }
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
        for (final Constructor<T> constructor : moduleClass.getConstructors())
        {
            constructors.add(constructor);
        }
        return constructors;
    }
}
