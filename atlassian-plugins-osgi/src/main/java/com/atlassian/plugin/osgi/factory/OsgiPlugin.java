package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.
 */
public class OsgiPlugin extends AbstractPlugin implements AutowireCapablePlugin, DynamicPlugin
{
    private final Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable = true;
    private boolean bundled = false;
    private Object nativeBeanFactory;
    private Method nativeCreateBeanMethod;
    private Method nativeAutowireBeanMethod;
    private ServiceTracker moduleDescriptorTracker;
    private ServiceTracker deferredModuleTracker;
    private final Map<String, Element> moduleElements = new HashMap<String, Element>();

    public OsgiPlugin(final Bundle bundle)
    {
        Validate.notNull(bundle, "The bundle is required");
        this.bundle = bundle;
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public void addModuleDescriptorElement(final String key, final Element element)
    {
        moduleElements.put(key, element);
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz, callingClass);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(final String name)
    {
        return BundleClassLoaderAccessor.getResource(bundle, name);
    }

    public InputStream getResourceAsStream(final String name)
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

    public void setDeletable(final boolean deletable)
    {
        this.deletable = deletable;
    }

    public boolean isBundledPlugin()
    {
        return bundled;
    }

    public void setBundled(final boolean bundled)
    {
        this.bundled = bundled;
    }

    @Override
    public synchronized boolean isEnabled()
    {
        return (Bundle.ACTIVE == bundle.getState()) && (!shouldHaveSpringContext() || ensureNativeBeanFactory());
    }

    @Override
    public synchronized void setEnabled(final boolean enabled) throws OsgiContainerException
    {
        if (enabled)
        {
            enable();
        }
        else
        {
            disable();
        }
    }

    void enable() throws OsgiContainerException
    {
        try
        {
            if ((bundle.getState() == Bundle.RESOLVED) || (bundle.getState() == Bundle.INSTALLED))
            {
                bundle.start();
                if (bundle.getBundleContext() != null)
                {
                    moduleDescriptorTracker = new ServiceTracker(bundle.getBundleContext(), ModuleDescriptor.class.getName(),
                        new RegisteringServiceTrackerCustomizer());
                    moduleDescriptorTracker.open();
                    deferredModuleTracker = new ServiceTracker(bundle.getBundleContext(), ListableModuleDescriptorFactory.class.getName(),
                        new DeferredServiceTrackerCustomizer());
                    deferredModuleTracker.open();
                }
            }
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Cannot start plugin: " + getKey(), e);
        }
    }

    void disable() throws OsgiContainerException
    {
        try
        {
            if (bundle.getState() == Bundle.ACTIVE)
            {
                if (moduleDescriptorTracker != null)
                {
                    moduleDescriptorTracker.close();
                }
                if (deferredModuleTracker != null)
                {
                    deferredModuleTracker.close();
                }
                bundle.stop();
                moduleDescriptorTracker = null;
                nativeBeanFactory = null;
                nativeCreateBeanMethod = null;
            }
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Cannot stop plugin: " + getKey(), e);
        }
    }

    public synchronized void close() throws OsgiContainerException
    {
        try
        {
            if (bundle.getState() != Bundle.UNINSTALLED)
            {
                bundle.uninstall();
            }
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Cannot uninstall bundle " + bundle.getSymbolicName());
        }
    }

    private boolean shouldHaveSpringContext()
    {
        return bundle.getHeaders().get("Spring-Context") != null;
    }

    public <T> T autowire(final Class<T> clazz)
    {
        return autowire(clazz, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    public synchronized <T> T autowire(final Class<T> clazz, final AutowireStrategy autowireStrategy)
    {
        if (!ensureNativeBeanFactory())
        {
            return null;
        }

        try
        {
            return (T) nativeCreateBeanMethod.invoke(nativeBeanFactory, clazz, autowireStrategy.ordinal(), false);
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

    public void autowire(final Object instance)
    {
        autowire(instance, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    public void autowire(final Object instance, final AutowireStrategy autowireStrategy)
    {
        if (!ensureNativeBeanFactory())
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

    private boolean ensureNativeBeanFactory()
    {
        if (nativeBeanFactory == null)
        {
            try
            {
                final BundleContext ctx = bundle.getBundleContext();
                if (ctx == null)
                {
                    log.warn("no bundle context - we are screwed");
                    return false;
                }
                final ServiceReference[] services = ctx.getServiceReferences("org.springframework.context.ApplicationContext",
                    "(org.springframework.context.service.name=" + bundle.getSymbolicName() + ")");
                if ((services == null) || (services.length == 0))
                {
                    log.debug("No spring bean factory found...yet");
                    return false;
                }

                final Object applicationContext = ctx.getService(services[0]);
                try
                {
                    final Method m = applicationContext.getClass().getMethod("getAutowireCapableBeanFactory");
                    nativeBeanFactory = m.invoke(applicationContext);
                }
                catch (final NoSuchMethodException e)
                {
                    // Should never happen
                    throw new PluginException("Cannot find createBean method on registered bean factory: " + nativeBeanFactory, e);
                }
                catch (final IllegalAccessException e)
                {
                    // Should never happen
                    throw new PluginException("Cannot access createBean method", e);
                }
                catch (final InvocationTargetException e)
                {
                    handleSpringMethodInvocationError(e);
                    return false;
                }
            }
            catch (final InvalidSyntaxException e)
            {
                throw new OsgiContainerException("Invalid LDAP filter", e);
            }

            try
            {
                nativeCreateBeanMethod = nativeBeanFactory.getClass().getMethod("createBean", Class.class, int.class, boolean.class);
                nativeAutowireBeanMethod = nativeBeanFactory.getClass().getMethod("autowireBeanProperties", Object.class, int.class, boolean.class);
            }
            catch (final NoSuchMethodException e)
            {
                // Should never happen
                throw new PluginException("Cannot find createBean method on registered bean factory: " + nativeBeanFactory, e);
            }
        }
        return (nativeBeanFactory != null) && (nativeCreateBeanMethod != null) && (nativeAutowireBeanMethod != null);
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

    @Override
    public String toString()
    {
        return getKey();
    }

    protected <T extends ModuleDescriptor> List<T> getModuleDescriptorsByDescriptorClass(final Class<T> descriptor)
    {
        final List<T> result = new ArrayList<T>();

        for (final ModuleDescriptor<?> moduleDescriptor : getModuleDescriptors())
        {
            if (moduleDescriptor.getClass().isAssignableFrom(descriptor))
            {
                result.add((T) moduleDescriptor);
            }
        }
        return result;
    }

    /**
     * Tracks module descriptors registered as services, then updates the descriptors map accordingly
     */
    private class RegisteringServiceTrackerCustomizer implements ServiceTrackerCustomizer
    {

        public Object addingService(final ServiceReference serviceReference)
        {
            ModuleDescriptor descriptor = null;
            if (serviceReference.getBundle() == bundle)
            {
                descriptor = (ModuleDescriptor) bundle.getBundleContext().getService(serviceReference);
                addModuleDescriptor(descriptor);
                log.info("Dynamically registered new module descriptor: " + descriptor.getCompleteKey());
            }
            return descriptor;
        }

        public void modifiedService(final ServiceReference serviceReference, final Object o)
        {
            if (serviceReference.getBundle() == bundle)
            {
                final ModuleDescriptor descriptor = (ModuleDescriptor) o;
                addModuleDescriptor(descriptor);
                log.info("Dynamically upgraded new module descriptor: " + descriptor.getCompleteKey());
            }
        }

        public void removedService(final ServiceReference serviceReference, final Object o)
        {
            if (serviceReference.getBundle() == bundle)
            {
                final ModuleDescriptor descriptor = (ModuleDescriptor) o;
                removeModuleDescriptor(descriptor.getKey());
                log.info("Dynamically removed module descriptor: " + descriptor.getCompleteKey());
            }
        }
    }

    /**
     * Service tracker that tracks {@link ListableModuleDescriptorFactory} instances and handles transforming
     * {@link UnrecognisedModuleDescriptor}} instances into modules if the new factory supports them.  Updates to factories
     * and removal are also handled.
     *
     * @since 2.1.2
     */
    private class DeferredServiceTrackerCustomizer implements ServiceTrackerCustomizer
    {

        /**
         * Turns any {@link UnrecognisedModuleDescriptor} modules that can be handled by the new factory into real
         * modules
         */
        public Object addingService(final ServiceReference serviceReference)
        {
            final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) bundle.getBundleContext().getService(serviceReference);
            for (final UnrecognisedModuleDescriptor deferred : getModuleDescriptorsByDescriptorClass(UnrecognisedModuleDescriptor.class))
            {
                final Element source = moduleElements.get(deferred.getKey());
                if ((source != null) && factory.hasModuleDescriptor(source.getName()))
                {
                    try
                    {
                        final ModuleDescriptor descriptor = factory.getModuleDescriptor(source.getName());
                        descriptor.init(deferred.getPlugin(), source);
                        addModuleDescriptor(descriptor);
                        log.info("Turned plugin module " + descriptor.getCompleteKey() + " into module " + descriptor);
                    }
                    catch (final IllegalAccessException e)
                    {
                        log.error("Unable to transform " + deferred.getKey() + " into actual plugin module using factory " + factory, e);
                    }
                    catch (final InstantiationException e)
                    {
                        log.error("Unable to transform " + deferred.getKey() + " into actual plugin module using factory " + factory, e);
                    }
                    catch (final ClassNotFoundException e)
                    {
                        log.error("Unable to transform " + deferred.getKey() + " into actual plugin module using factory " + factory, e);
                    }
                }
            }
            return factory;
        }

        /**
         * Updates any local module descriptors that were created from the modified factory
         */
        public void modifiedService(final ServiceReference serviceReference, final Object o)
        {
            removedService(serviceReference, o);
            addingService(serviceReference);
        }

        /**
         * Reverts any current module descriptors that were provided from the factory being removed into {@link
         * UnrecognisedModuleDescriptor} instances.
         */
        public void removedService(final ServiceReference serviceReference, final Object o)
        {
            final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) o;
            for (final Class<ModuleDescriptor<?>> moduleDescriptorClass : factory.getModuleDescriptorClasses())
            {
                for (final ModuleDescriptor<?> descriptor : getModuleDescriptorsByDescriptorClass(moduleDescriptorClass))
                {
                    final UnrecognisedModuleDescriptor deferred = new UnrecognisedModuleDescriptor();
                    final Element source = moduleElements.get(descriptor.getKey());
                    if (source != null)
                    {
                        deferred.init(OsgiPlugin.this, source);
                        deferred.setErrorText(UnrecognisedModuleDescriptorFallbackFactory.DESCRIPTOR_TEXT);
                        addModuleDescriptor(deferred);
                        log.info("Removed plugin module " + deferred.getCompleteKey() + " as its factory was uninstalled");
                    }
                }
            }
        }
    }
}
