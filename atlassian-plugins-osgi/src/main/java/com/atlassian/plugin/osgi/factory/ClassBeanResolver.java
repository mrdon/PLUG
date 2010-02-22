package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.BeanResolver;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * TODO: Document this class / interface here
 */
public class ClassBeanResolver implements BeanResolver
{
    private volatile SpringContextAccessor springContextAccessor;

    public ClassBeanResolver()
    {
        
    }

    public boolean supportsPrefix(final String prefix)
    {
        return "class".equals(prefix);
    }

    public Object resolveNameToObject(final String name)
    {
        return null;
    }

    public void autowire(final Object object, final Plugin plugin)
    {
    }

    public void setPluginContainer(final Object container)
    {
        if (container == null)
        {
            springContextAccessor = null;
        }
        else
        {
            springContextAccessor = new SpringContextAccessor(container);
        }
    }

    /**
     * Manages spring context access, including autowiring.
     *
     * @since 2.2.0
     */
    private static final class SpringContextAccessor
    {
        private final Object nativeBeanFactory;
        private final Method nativeCreateBeanMethod;
        private final Method nativeAutowireBeanMethod;

        public SpringContextAccessor(final Object applicationContext)
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

        public <T> T createBean(final Class<T> clazz, final AutowireCapablePlugin.AutowireStrategy autowireStrategy)
        {
            if (nativeBeanFactory == null)
            {
                return null;
            }

            try
            {
                return clazz.cast(nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, autowireStrategy.ordinal(), false));
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

        public void createBean(final Object instance, final AutowireCapablePlugin.AutowireStrategy autowireStrategy)
        {
            if (nativeBeanFactory == null)
            {
                return;
            }

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
    }
}
