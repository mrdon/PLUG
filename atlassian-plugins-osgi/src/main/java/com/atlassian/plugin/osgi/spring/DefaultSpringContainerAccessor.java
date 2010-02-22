package com.atlassian.plugin.osgi.spring;

import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.module.ContainerAccessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.Validate;

/**
 * Manages spring context access, including autowiring.
 *
 * @since 2.2.0
 */
public class DefaultSpringContainerAccessor implements SpringContainerAccessor
{
    private final Object nativeBeanFactory;
    private final Method nativeCreateBeanMethod;
    private final Method nativeAutowireBeanMethod;
    private final Method nativeGetBeanMethod;

    /**
     * The autowire strategy to use when creating and wiring a bean
     */
    private enum AutowireStrategy
    {
        AUTOWIRE_NO,
        /** Performs setter-based injection by name */
        AUTOWIRE_BY_NAME,

        /** Performs setter-based injection by type */
        AUTOWIRE_BY_TYPE,

        /** Performs construction-based injection by type */
        AUTOWIRE_BY_CONSTRUCTOR,

        /**
         * Autodetects appropriate injection by first seeing if any no-arg constructors exist.  If not, performs constructor
         * injection, and if so, autowires by type then name
         */
        AUTOWIRE_AUTODETECT
    }

    public DefaultSpringContainerAccessor(final Object applicationContext)
    {
        Object beanFactory = null;
        try
        {
            final Method m = applicationContext.getClass().getMethod("getAutowireCapableBeanFactory");
            beanFactory = m.invoke(applicationContext);
        }
        catch (final NoSuchMethodException e)
        {
            // Should never happen
            throw new PluginException("Cannot find createBean method on registered bean factory: " + beanFactory, e);
        }
        catch (final IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Cannot access createBean method", e);
        }
        catch (final InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
        }

        nativeBeanFactory = beanFactory;
        try
        {
            nativeCreateBeanMethod = beanFactory.getClass().getMethod("createBean", Class.class, int.class, boolean.class);
            nativeAutowireBeanMethod = beanFactory.getClass().getMethod("autowireBeanProperties", Object.class, int.class, boolean.class);
            nativeGetBeanMethod = beanFactory.getClass().getMethod("getBean", String.class);

            Validate.noNullElements(new Object[] {nativeAutowireBeanMethod, nativeCreateBeanMethod, nativeGetBeanMethod});
        }
        catch (final NoSuchMethodException e)
        {
            // Should never happen
            throw new PluginException("Cannot find createBean method on registered bean factory: " + nativeBeanFactory, e);
        }
    }

    private void handleSpringMethodInvocationError(final InvocationTargetException e)
    {
        if (e.getCause() instanceof Error)
        {
            throw (Error) e.getCause();
        }
        else if (e.getCause() instanceof RuntimeException)
        {
            throw (RuntimeException) e.getCause();
        }
        else
        {
            // Should never happen as Spring methods only throw runtime exceptions
            throw new PluginException("Unable to invoke createBean", e.getCause());
        }
    }

    public <T> T createBean(final Class<T> clazz)
    {
        try
        {
            return clazz.cast(nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, AutowireStrategy.AUTOWIRE_AUTODETECT.ordinal(), false));
        }
        catch (final IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (final InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }

    public void autowireBean(final Object instance, AutowireCapablePlugin.AutowireStrategy autowireStrategy)
    {
        try
        {
            nativeAutowireBeanMethod.invoke(nativeBeanFactory, instance, autowireStrategy.ordinal(), false);
        }
        catch (final IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (final InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
        }
    }

    public Object getBean(String id)
    {
        try
        {
            return nativeGetBeanMethod.invoke(nativeBeanFactory, id);
        }
        catch (final IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Unable to access getBean method", e);
        }
        catch (final InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }
}