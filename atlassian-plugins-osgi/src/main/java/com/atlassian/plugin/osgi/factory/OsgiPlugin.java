package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.osgi.container.OsgiContainerException;

import java.net.URL;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
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
    private Method nativeAutowireBeanMethod;
    private Map<String,ModuleDescriptor<?>> modules = new ConcurrentHashMap<String,ModuleDescriptor<?>>();
    private ServiceTracker moduleDescriptorTracker;

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
                if (bundle.getBundleContext() != null)
                {
                    moduleDescriptorTracker = new ServiceTracker(bundle.getBundleContext(), ModuleDescriptor.class.getName(), new RegisteringServiceTrackerCustomizer());
                    moduleDescriptorTracker.open();
                }
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
                if (moduleDescriptorTracker != null) moduleDescriptorTracker.close();
                bundle.stop();
                moduleDescriptorTracker = null;
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

    public void addModuleDescriptor(ModuleDescriptor<?> moduleDescriptor)
    {
        modules.put(moduleDescriptor.getKey(), moduleDescriptor);
    }

    public Collection<ModuleDescriptor<?>> getModuleDescriptors()
    {
        return modules.values();
    }

    public ModuleDescriptor<?> getModuleDescriptor(String key)
    {
        return modules.get(key);
    }

    public <T> List<ModuleDescriptor<T>> getModuleDescriptorsByModuleClass(Class<T> aClass)
    {
        List<ModuleDescriptor<T>> result = new ArrayList<ModuleDescriptor<T>>();

        for (ModuleDescriptor moduleDescriptor : modules.values())
        {
            Class moduleClass = moduleDescriptor.getModuleClass();
            if (aClass.isAssignableFrom(moduleClass))
            {
                result.add((ModuleDescriptor<T>) moduleDescriptor);
            }
        }

        return result;
    }

    public boolean containsSystemModule()
    {
        for (Iterator iterator = modules.values().iterator(); iterator.hasNext();)
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator.next();
            if(moduleDescriptor.isSystemModule())
            {
                return true;
            }
        }
        return false;
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

    public void autowire(Object instance)
    {
        autowire(instance, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    public void autowire(Object instance, AutowireStrategy autowireStrategy)
    {
        if (!ensureNativeBeanFactory())
            return;

        try
        {
            nativeAutowireBeanMethod.invoke(nativeBeanFactory, instance, autowireStrategy.ordinal(), false);
        }
        catch (IllegalAccessException e)
        {
            // Should never happen
            throw new PluginException("Unable to access createBean method", e);
        }
        catch (InvocationTargetException e)
        {
            handleSpringMethodInvocationError(e);
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
                nativeAutowireBeanMethod = nativeBeanFactory.getClass().getMethod("autowireBeanProperties", Object.class, int.class, boolean.class);
            } catch (NoSuchMethodException e)
            {
                // Should never happen
                throw new PluginException("Cannot find createBean method on registered bean factory: "+nativeBeanFactory, e);
            }
        }
        return nativeBeanFactory != null && nativeCreateBeanMethod != null && nativeAutowireBeanMethod != null;
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

    /**
     * Tracks module descriptors registered as services, then updates the descriptors map accordingly
     */
    private class RegisteringServiceTrackerCustomizer implements ServiceTrackerCustomizer
    {

        public Object addingService(ServiceReference serviceReference)
        {
            if (serviceReference.getBundle() == bundle)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) bundle.getBundleContext().getService(serviceReference);
                modules.put(descriptor.getKey(), descriptor);
                return descriptor;
            }
            return null;
        }

        public void modifiedService(ServiceReference serviceReference, Object o)
        {
            if (serviceReference.getBundle() == bundle)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) o;
                modules.put(descriptor.getKey(), descriptor);
            }
        }

        public void removedService(ServiceReference serviceReference, Object o)
        {
            if (serviceReference.getBundle() == bundle)
            {
                ModuleDescriptor descriptor = (ModuleDescriptor) o;
                modules.remove(descriptor.getKey());
            }
        }
    }
}
