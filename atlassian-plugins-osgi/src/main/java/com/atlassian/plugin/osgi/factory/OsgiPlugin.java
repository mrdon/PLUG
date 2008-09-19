package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.osgi.container.OsgiContainerException;

import java.net.URL;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.osgi.framework.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.
 */
public class OsgiPlugin extends AbstractPlugin implements AutowireCapablePlugin, DynamicPlugin
{
    private Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable = true;
    private boolean bundled = false;
    private Object nativeBeanFactory;
    private Method nativeCreateBeanMethod;

    public OsgiPlugin(Bundle bundle)
    {
        Validate.notNull(bundle, "The bundle is required");
        this.bundle = bundle;
    }

    public Bundle getBundle()
    {
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

    public synchronized boolean isEnabled()
    {
        return Bundle.ACTIVE == bundle.getState() && (!shouldHaveSpringContext() || ensureNativeBeanFactory());
    }

    public synchronized void setEnabled(boolean enabled) throws OsgiContainerException
    {
        if (enabled) enable(); else disable();
    }

    void enable() throws OsgiContainerException
    {
        try
        {
            if (bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.INSTALLED)
            {
                bundle.start();
            }
        }
        catch (BundleException e)
        {
            throw new OsgiContainerException("Cannot start plugin: "+getKey(), e);
        }
    }

    void disable() throws OsgiContainerException
    {
        try
        {
            if (bundle.getState() == Bundle.ACTIVE)
            {
                bundle.stop();
                nativeBeanFactory = null;
                nativeCreateBeanMethod = null;
            }
        } catch (BundleException e)
        {
            throw new OsgiContainerException("Cannot stop plugin: "+getKey(), e);
        }
    }

    public synchronized void close() throws OsgiContainerException
    {
        try
        {
            if (bundle.getState() != Bundle.UNINSTALLED)
                bundle.uninstall();
        } catch (BundleException e)
        {
            throw new OsgiContainerException("Cannot uninstall bundle " + bundle.getSymbolicName());
        }
    }

    private boolean shouldHaveSpringContext()
    {
        return bundle.getHeaders().get("Spring-Context") != null;
    }

    public <T> T autowire(Class<T> clazz)
    {
        return autowire(clazz, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    public synchronized <T> T autowire(Class<T> clazz, AutowireStrategy autowireStrategy)
    {
        if (!ensureNativeBeanFactory())
            return null;
        
        try
        {
            return (T) nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, autowireStrategy.ordinal(), false);
        }
        catch (IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
            return null;
        }
    }

    private boolean ensureNativeBeanFactory()
    {
        if (nativeBeanFactory == null)
        {

            try
            {
                BundleContext ctx = bundle.getBundleContext();
                if (ctx == null)
                {
                    log.warn("no bundle context - we are screwed");
                    return false;
                }
                ServiceReference[] services = ctx.getServiceReferences("org.springframework.context.ApplicationContext", "(org.springframework.context.service.name="+bundle.getSymbolicName()+")");
                if (services == null || services.length == 0)
                {
                    log.debug("No spring bean factory found...yet");
                    return false;
                }

                Object applicationContext = ctx.getService(services[0]);
                try
                {
                    Method m = applicationContext.getClass().getMethod("getAutowireCapableBeanFactory");
                    nativeBeanFactory = m.invoke(applicationContext);
                } catch (NoSuchMethodException e)
                {
                    // Should never happen
                    throw new PluginException("Cannot find createBean method on registered bean factory: "+nativeBeanFactory, e);
                } catch (IllegalAccessException e)
                {
                    // Should never happen
                    throw new PluginException("Cannot access createBean method", e);
                } catch (InvocationTargetException e)
                {
                    handleSpringMethodInvocationError(e);
                    return false;
                }
            } catch (InvalidSyntaxException e)
            {
                throw new OsgiContainerException("Invalid LDAP filter", e);
            }


            try
            {
                nativeCreateBeanMethod = nativeBeanFactory.getClass().getMethod("createBean", Class.class, int.class, boolean.class);
            } catch (NoSuchMethodException e)
            {
                // Should never happen
                throw new PluginException("Cannot find createBean method on registered bean factory: "+nativeBeanFactory, e);
            }
        }
        return nativeBeanFactory != null && nativeCreateBeanMethod != null;
    }

    private void handleSpringMethodInvocationError(InvocationTargetException e)
    {
        if (e.getCause() instanceof Error)
            throw (Error) e.getCause();
        else if (e.getCause() instanceof RuntimeException)
            throw (RuntimeException) e.getCause();
        else
        {
            // Should never happen as Spring methods only throw runtime exceptions
            throw new PluginException("Unable to invoke createBean", e.getCause());
        }
    }

    public String toString()
    {
        return getKey();
    }

}
