package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.AutowireCapablePlugin;

import java.net.URL;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.
 */
public class OsgiPlugin extends AbstractPlugin implements StateAware, AutowireCapablePlugin, DynamicPlugin
{
    private Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable = true;
    private boolean bundled = false;
    private Object nativeBeanFactory;
    private Method nativeCreateBeanMethod;

    public OsgiPlugin(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz, callingClass);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(String name)
    {
        return BundleClassLoaderAccessor.getResource(bundle, name);
    }

    public InputStream getResourceAsStream(String name)
    {
        return BundleClassLoaderAccessor.getResourceAsStream(bundle, name);
    }

    public ClassLoader getClassLoader()
    {
        return BundleClassLoaderAccessor.getClassLoader(bundle);
    }

    /**
     * This plugin is dynamically loaded, so returns true.
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
    }


    public boolean isDeleteable()
    {
        return deletable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public boolean isBundledPlugin()
    {
        return bundled;
    }

    public void setBundled(boolean bundled)
    {
        this.bundled = bundled;
    }

    public boolean isEnabled()
    {
        return Bundle.ACTIVE == bundle.getState();
    }

    public void setEnabled(boolean enabled)
    {
        if (enabled) {
            enabled();
        }
        else {
            disabled();
        }
    }

    public void enabled()
    {
        try
        {
            bundle.start();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot start plugin: "+getKey(), e);
        }
    }

    public void disabled()
    {
        try
        {
            bundle.stop();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot stop plugin: "+getKey(), e);
        }
    }

    public void close()
    {
        try
        {
            bundle.uninstall();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot uninstall bundle " + bundle.getSymbolicName());
        }
    }

    public <T> T autowireGeneric(Class<T> clazz)
    {
        return autowire(clazz, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    public <T> T autowire(Class<T> clazz, AutowireStrategy autowireStrategy)
    {
        if (nativeBeanFactory == null)
        {

            try
            {
                BundleContext ctx = bundle.getBundleContext();
                if (ctx == null)
                {
                    log.warn("no bundle context - we are screwed");
                    return null;
                }
                ServiceReference[] services = ctx.getServiceReferences("org.springframework.context.ApplicationContext", "(org.springframework.context.service.name="+bundle.getSymbolicName()+")");
                if (services == null || services.length == 0)
                {
                    log.warn("No spring bean factory found");
                    return null;
                }

                Object applicationContext = ctx.getService(services[0]);
                try
                {
                    Method m = applicationContext.getClass().getMethod("getAutowireCapableBeanFactory");
                    nativeBeanFactory = m.invoke(applicationContext);
                } catch (NoSuchMethodException e)
                {
                    // Should never happen
                    throw new RuntimeException("Cannot find createBean method on registered bean factory: "+nativeBeanFactory, e);
                } catch (IllegalAccessException e)
                {
                    throw new RuntimeException("Cannot access createBean method", e);
                } catch (InvocationTargetException e)
                {
                    throw new RuntimeException("Cannot invoke createBean method", e.getCause());
                }
            } catch (InvalidSyntaxException e)
            {
                // should never happen
                throw new RuntimeException("Invalid LDAP filter", e);
            }


            try
            {
                nativeCreateBeanMethod = nativeBeanFactory.getClass().getMethod("createBean", Class.class, int.class, boolean.class);
            } catch (NoSuchMethodException e)
            {
                // Should never happen
                throw new RuntimeException("Cannot find createBean method on registered bean factory: "+nativeBeanFactory, e);
            }
        }
        try
        {
            return (T) nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, autowireStrategy.ordinal(), false);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Unable to access bean:" + getExceptionMessage(e), e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException("Unable to call beanfactory method:" + getExceptionMessage(e), e);
        }
    }

    private String getExceptionMessage(Exception e) {
        return e.getMessage() == null ? e.getCause().getMessage() : e.getMessage();
    }

    public Object autowire(Class clazz) {
        return autowireGeneric(clazz);
    }

    public Object autowire(Class clazz, int autowireStrategy) {
        return autowire(clazz, AutowireStrategy.fromIndex(autowireStrategy));
    }
}
