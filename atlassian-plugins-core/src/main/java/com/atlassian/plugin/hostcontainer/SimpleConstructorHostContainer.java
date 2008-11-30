package com.atlassian.plugin.hostcontainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Constructs module instances, matching the constructor with the largest number of arguments first.  The objects to
 * pass to the constructor are retrieved from the passed map of classes and objects.  The classes are matched on an
 * exact class match.
 *
 * @since 2.2.0
 */
public class SimpleConstructorHostContainer implements HostContainer
{
    private final Map<Class, Object> context;

    public SimpleConstructorHostContainer(Map<Class, Object> context)
    {
        Map<Class, Object> tmp = new HashMap<Class, Object>(context);
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
    public <T> T create(Class<T> moduleClass) throws IllegalArgumentException
    {
        for (Constructor<T> constructor : findConstructorsLargestFirst(moduleClass))
        {
            List<Object> params = new ArrayList<Object>();
            for (Class paramType : constructor.getParameterTypes())
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
            catch (InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch (InvocationTargetException e)
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
    public <T> T getInstance(Class<T> moduleClass)
    {
        return (T) context.get(moduleClass);
    }

    private <T> Collection<Constructor<T>> findConstructorsLargestFirst(Class<T> moduleClass)
    {
        Set<Constructor<T>> constructors = new TreeSet<Constructor<T>>(new Comparator<Constructor<T>>()
        {
            public int compare(Constructor<T> first, Constructor<T> second)
            {
                return Integer.valueOf(second.getParameterTypes().length).compareTo(first.getParameterTypes().length);
            }

            public boolean equals(Constructor o)
            {
                return false;
            }
        });
        for (Constructor constructor : moduleClass.getConstructors())
        {
            constructors.add((Constructor<T>) constructor);
        }
        return constructors;
    }
}
