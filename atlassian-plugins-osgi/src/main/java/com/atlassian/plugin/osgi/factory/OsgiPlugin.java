package com.atlassian.plugin.osgi.factory;

import static org.apache.commons.lang.Validate.notNull;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginContainerFailedEvent;
import com.atlassian.plugin.event.events.PluginContainerRefreshedEvent;
import com.atlassian.plugin.event.events.PluginRefreshedEvent;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.resource.AlternativeDirectoryResourceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.  The actual bundle is not created until the
 * {@link #install()} method is invoked.  Any attempt to access a method that requires a bundle will throw an
 * {@link IllegalStateException}.
 */
//@Threadsafe
public class OsgiPlugin extends AbstractPlugin implements AutowireCapablePlugin, DynamicPlugin
{
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);

    private final Map<String, Element> moduleElements = new HashMap<String, Element>();
    private final PluginEventManager pluginEventManager;
    private final OsgiContainerManager osgiContainerManager;
    private final PluginArtifact pluginArtifact;
    private final PackageAdmin packageAdmin;
    private volatile Bundle bundle;
    private volatile SpringContextAccessor springContextAccessor;
    private volatile ClassLoader bundleClassLoader;
    private volatile boolean treatSpringBeanFactoryCreationAsRefresh = false;
    private volatile boolean deletable = true;
    private volatile boolean bundled = false;
    //@GuardedBy("this")
    private ServiceTracker moduleDescriptorTracker;
    //@GuardedBy("this")
    private ServiceTracker unrecognisedModuleTracker;

    public OsgiPlugin(final OsgiContainerManager osgiContainerManager, final PluginArtifact pluginArtifact, final PluginEventManager pluginEventManager)
    {
        notNull(osgiContainerManager, "The osgiContainerManager is required");
        notNull(pluginArtifact, "The pluginArtifact is required");
        notNull(pluginEventManager, "The pluginEventManager is required");
        this.osgiContainerManager = osgiContainerManager;
        this.pluginArtifact = pluginArtifact;
        this.pluginEventManager = pluginEventManager;

        final Bundle systemBundle = osgiContainerManager.getBundles()[0];
        final BundleContext systemBundleContext = systemBundle.getBundleContext();
        final ServiceReference ref = systemBundleContext.getServiceReference(PackageAdmin.class.getName());
        packageAdmin = (PackageAdmin) systemBundleContext.getService(ref);
    }

    /**
     * Only useful for testing
     */
    OsgiPlugin(final Bundle bundle)
    {
        this.bundle = bundle;
        osgiContainerManager = null;
        pluginArtifact = null;
        pluginEventManager = new DefaultPluginEventManager();
        packageAdmin = null;
    }

    /**
     * @return The active bundle
     *
     * @throws IllegalStateException If the bundle hasn't been installed yet
     */
    public Bundle getBundle() throws IllegalStateException
    {
        if (bundle == null)
        {
            throw new IllegalStateException("Bundle hasn't been created yet.  This is probably because the module " +
                "descriptor is trying to load classes in its init() method.  Move all classloading into the " +
                "enabled() method, and be sure to properly drop class and instance references in disabled().");
        }
        return bundle;
    }

    public void addModuleDescriptorElement(final String key, final Element element)
    {
        moduleElements.put(key, element);
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(getBundle(), clazz, callingClass);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(final String name)
    {
        return bundleClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(final String name)
    {
        return bundleClassLoader.getResourceAsStream(name);
    }

    public ClassLoader getClassLoader()
    {
        return bundleClassLoader;
    }

    /**
     * @param key The plugin key
     * @throws IllegalArgumentException If the plugin key doesn't match the bundle key
     */
    @Override
    public void setKey(final String key) throws IllegalArgumentException
    {
        if ((bundle != null) && !bundle.getSymbolicName().equals(key))
        {
            throw new IllegalArgumentException("The plugin key '"+key+"' must match the OSGi bundle symbolic name (Bundle-SymbolicName)");
        }
        super.setKey(key);
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

    @PluginEventListener
    public void onSpringContextFailed(final PluginContainerFailedEvent event)
    {
        if (getKey() == null)
        {
            throw new IllegalStateException("Plugin key must be set");
        }
        if (getKey().equals(event.getPluginKey()))
        {
            // TODO: do something with the exception more than logging
            log.error("Unable to start the Spring context for plugin "+getKey(), event.getCause());
            setPluginState(PluginState.DISABLED);
        }
    }

    @PluginEventListener
    public void onSpringContextRefresh(final PluginContainerRefreshedEvent event)
    {
        if (getKey() == null)
        {
            throw new IllegalStateException("Plugin key must be set");
        }
        if (getKey().equals(event.getPluginKey()))
        {
            springContextAccessor = new SpringContextAccessor(event.getContainer());
            setPluginState(PluginState.ENABLED);

            // Only send refresh event on second creation
            if (treatSpringBeanFactoryCreationAsRefresh)
            {
                pluginEventManager.broadcast(new PluginRefreshedEvent(this));
            }
            else
            {
                treatSpringBeanFactoryCreationAsRefresh = true;
            }
        }
    }

    @Override
    public void install()
    {
        bundle = osgiContainerManager.installBundle(pluginArtifact.toFile());
        bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        setPluginState(PluginState.INSTALLED);
    }

    @Override
    public synchronized void enable() throws OsgiContainerException
    {
        try
        {
            if ((getBundle().getState() == Bundle.RESOLVED) || (getBundle().getState() == Bundle.INSTALLED))
            {
                pluginEventManager.register(this);
                getBundle().start();
                if (getBundle().getBundleContext() != null)
                {
                    if (shouldHaveSpringContext() && (springContextAccessor == null))
                    {
                        setPluginState(PluginState.ENABLING);
                    }
                    else
                    {
                        setPluginState(PluginState.ENABLED);
                    }
                    final BundleContext ctx = getBundle().getBundleContext();
                    moduleDescriptorTracker = new ServiceTracker(ctx, ModuleDescriptor.class.getName(),
                        new RegisteringServiceTrackerCustomizer());
                    moduleDescriptorTracker.open();
                    unrecognisedModuleTracker = new ServiceTracker(ctx, ListableModuleDescriptorFactory.class.getName(),
                        new UnrecognisedServiceTrackerCustomizer());
                    unrecognisedModuleTracker.open();

                    // ensure the bean factory is removed when the bundle is stopped
                    // Do we need to unregister this?
                    ctx.addBundleListener(new BundleListener()
                    {
                        public void bundleChanged(final BundleEvent bundleEvent)
                        {
                            if ((bundleEvent.getBundle() == getBundle()) && (bundleEvent.getType() == BundleEvent.STOPPED))
                            {
                                springContextAccessor = null;
                                setPluginState(PluginState.DISABLED);
                            }
                        }
                    });
                }
                else
                {
                    log.warn("Bundle started, but no BundleContext available.  This should never happen outside tests.");
                }
            }
            else
            {
                log.warn("Cannot enable a plugin that is already enabled or in the process of being enabled.");
            }
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Cannot start plugin: " + getKey(), e);
        }
    }

    @Override
    public synchronized void disable() throws OsgiContainerException
    {
        try
        {
            if (getBundle().getState() == Bundle.ACTIVE)
            {
                // Only disable underlying bundle if this is a truely dynamic plugin
                if (!PluginUtils.doesPluginRequireRestart(this))
                {
                    if (moduleDescriptorTracker != null)
                    {
                        moduleDescriptorTracker.close();
                    }
                    if (unrecognisedModuleTracker != null)
                    {
                        unrecognisedModuleTracker.close();
                    }
                    pluginEventManager.unregister(this);
                    getBundle().stop();
                    moduleDescriptorTracker = null;
                    unrecognisedModuleTracker = null;
                }
                setPluginState(PluginState.DISABLED);
            }
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Cannot stop plugin: " + getKey(), e);
        }
    }

    @Override
    public void uninstall() throws OsgiContainerException
    {
        if (bundle != null)
        {
            try
            {
                if (bundle.getState() != Bundle.UNINSTALLED)
                {
                    pluginEventManager.unregister(this);
                    bundle.uninstall();
                    setPluginState(PluginState.UNINSTALLED);
                }
            }
            catch (final BundleException e)
            {
                throw new OsgiContainerException("Cannot uninstall bundle " + bundle.getSymbolicName());
            }
        }
    }

    private boolean shouldHaveSpringContext()
    {
        return getBundle().getHeaders().get("Spring-Context") != null;
    }

    /**
     * @throws IllegalStateException if the spring context is not initialized
     */
    public <T> T autowire(final Class<T> clazz) throws IllegalStateException
    {
        return autowire(clazz, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    /**
     * @throws IllegalStateException if the spring context is not initialized
     */
    public <T> T autowire(final Class<T> clazz, final AutowireStrategy autowireStrategy) throws IllegalStateException
    {
        assertSpringContextAvailable();
        return springContextAccessor.createBean(clazz, autowireStrategy);
    }

    /**
     * @throws IllegalStateException if the spring context is not initialized
     */
    public void autowire(final Object instance) throws IllegalStateException
    {
        autowire(instance, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    /**
     * @throws IllegalStateException if the spring context is not initialized
     */
    public void autowire(final Object instance, final AutowireStrategy autowireStrategy) throws IllegalStateException
    {
        assertSpringContextAvailable();
        springContextAccessor.createBean(instance, autowireStrategy);
    }

    /**
     * @throws IllegalStateException if the spring context is not initialized
     */
    private void assertSpringContextAvailable() throws IllegalStateException
    {
        if (springContextAccessor == null)
        {
            throw new IllegalStateException("Cannot autowire object because the Spring context is unavailable.  " +
                "Ensure your OSGi bundle contains the 'Spring-Context' header.");
        }
    }


    Map<String, Element> getModuleElements()
    {
        return moduleElements;
    }

    @Override
    public String toString()
    {
        return getKey();
    }

    protected <T extends ModuleDescriptor<?>> List<T> getModuleDescriptorsByDescriptorClass(final Class<T> descriptor)
    {
        final List<T> result = new ArrayList<T>();

        for (final ModuleDescriptor<?> moduleDescriptor : getModuleDescriptors())
        {
            if (moduleDescriptor.getClass().isAssignableFrom(descriptor))
            {
                result.add(descriptor.cast(moduleDescriptor));
            }
        }
        return result;
    }

    /**
     * Use the PackageAdmin to determine which bundles provide the package imports
     *
     * @return A set of bundle symbolic names, or plugin keys.  Empty set if none.
     * @since 2.2.0
     */
    @Override
    public Set<String> getRequiredPlugins()
    {
        final Set<String> keys = new HashSet<String>();

        final Set<String> imports = OsgiHeaderUtil.parseHeader((String) getBundle().getHeaders().get(Constants.IMPORT_PACKAGE)).keySet();
        for (final String imp : imports)
        {
            final ExportedPackage[] exports = packageAdmin.getExportedPackages(imp);
            if (exports != null)
            {
                for (final ExportedPackage export : exports)
                {
                    final Bundle[] importingBundles = export.getImportingBundles();
                    if (importingBundles != null)
                    {
                        for (final Bundle importingBundle : importingBundles)
                        {
                            if (getBundle() == importingBundle)
                            {
                                keys.add(export.getExportingBundle().getSymbolicName());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Tracks module descriptors registered as services, then updates the descriptors map accordingly
     */
    private class RegisteringServiceTrackerCustomizer implements ServiceTrackerCustomizer
    {

        public Object addingService(final ServiceReference serviceReference)
        {
            ModuleDescriptor<?> descriptor = null;
            if (serviceReference.getBundle() == getBundle())
            {
                descriptor = (ModuleDescriptor<?>) getBundle().getBundleContext().getService(serviceReference);
                addModuleDescriptor(descriptor);
                if (log.isInfoEnabled())
                {
                    log.info("Dynamically registered new module descriptor: " + descriptor.getCompleteKey());
                }
            }
            return descriptor;
        }

        public void modifiedService(final ServiceReference serviceReference, final Object o)
        {
            if (serviceReference.getBundle() == getBundle())
            {
                final ModuleDescriptor<?> descriptor = (ModuleDescriptor<?>) o;
                addModuleDescriptor(descriptor);
                if (log.isInfoEnabled())
                {
                    log.info("Dynamically upgraded new module descriptor: " + descriptor.getCompleteKey());
                }
            }
        }

        public void removedService(final ServiceReference serviceReference, final Object o)
        {
            if (serviceReference.getBundle() == getBundle())
            {
                final ModuleDescriptor<?> descriptor = (ModuleDescriptor<?>) o;
                removeModuleDescriptor(descriptor.getKey());
                if (log.isInfoEnabled())
                {
                    log.info("Dynamically removed module descriptor: " + descriptor.getCompleteKey());
                }
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
    private class UnrecognisedServiceTrackerCustomizer implements ServiceTrackerCustomizer
    {

        /**
         * Turns any {@link UnrecognisedModuleDescriptor} modules that can be handled by the new factory into real
         * modules
         */
        public Object addingService(final ServiceReference serviceReference)
        {
            final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) getBundle().getBundleContext().getService(serviceReference);
            for (final UnrecognisedModuleDescriptor unrecognised : getModuleDescriptorsByDescriptorClass(UnrecognisedModuleDescriptor.class))
            {
                final Element source = moduleElements.get(unrecognised.getKey());
                if ((source != null) && factory.hasModuleDescriptor(source.getName()))
                {
                    try
                    {
                        final ModuleDescriptor<?> descriptor = factory.getModuleDescriptor(source.getName());
                        descriptor.init(unrecognised.getPlugin(), source);
                        addModuleDescriptor(descriptor);
                        if (log.isInfoEnabled())
                        {
                            log.info("Turned plugin module " + descriptor.getCompleteKey() + " into module " + descriptor);
                        }
                    }
                    catch (final Exception e)
                    {
                        log.error("Unable to transform " + unrecognised.getCompleteKey() + " into actual plugin module using factory " + factory, e);
                        unrecognised.setErrorText(e.getMessage());
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
                    final UnrecognisedModuleDescriptor unrecognisedModuleDescriptor = new UnrecognisedModuleDescriptor();
                    final Element source = moduleElements.get(descriptor.getKey());
                    if (source != null)
                    {
                        unrecognisedModuleDescriptor.init(OsgiPlugin.this, source);
                        unrecognisedModuleDescriptor.setErrorText(UnrecognisedModuleDescriptorFallbackFactory.DESCRIPTOR_TEXT);
                        addModuleDescriptor(unrecognisedModuleDescriptor);
                        if (log.isInfoEnabled())
                        {
                            log.info("Removed plugin module " + unrecognisedModuleDescriptor.getCompleteKey() + " as its factory was uninstalled");
                        }
                    }
                }
            }
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

        public <T> T createBean(final Class<T> clazz, final AutowireStrategy autowireStrategy)
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

        public void createBean(final Object instance, final AutowireStrategy autowireStrategy)
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
