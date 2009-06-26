package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.IllegalPluginStateException;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.osgi.util.BundleClassLoaderAccessor;
import com.atlassian.plugin.util.resource.AlternativeDirectoryResourceLoader;

import org.apache.commons.lang.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that implements the methods assuming the OSGi plugin has been installed
 *
 * @since 2.2.0
 */
class OsgiPluginInstalledHelper implements OsgiPluginHelper
{
    private final ClassLoader bundleClassLoader;
    private final Bundle bundle;
    private final PackageAdmin packageAdmin;
    private final boolean requireSpring;

    private volatile SpringContextAccessor springContextAccessor;
    private ServiceTracker[] serviceTrackers;

    /**
     * @param bundle The bundle
     * @param packageAdmin The package admin
     * @param requireSpring Whether spring is required for autowiring
     */
    public OsgiPluginInstalledHelper(final Bundle bundle, final PackageAdmin packageAdmin, final boolean requireSpring)
    {
        Validate.notNull(bundle);
        Validate.notNull(packageAdmin);
        this.bundle = bundle;
        bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        this.packageAdmin = packageAdmin;
        this.requireSpring = requireSpring;
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(getBundle(), clazz);
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

    public Bundle install()
    {
        throw new IllegalPluginStateException("Plugin '" + bundle.getSymbolicName() + "' has already been installed");
    }

    public void onEnable(final ServiceTracker... serviceTrackers) throws OsgiContainerException
    {
        Validate.notNull(serviceTrackers);
        this.serviceTrackers = serviceTrackers;
        for (final ServiceTracker svc : serviceTrackers)
        {
            svc.open();
        }
    }

    public void onDisable() throws OsgiContainerException
    {
        for (final ServiceTracker svc : serviceTrackers)
        {
            svc.close();
        }
        serviceTrackers = null;
        setPluginContainer(null);
    }

    public void onUninstall() throws OsgiContainerException
    {
    }

    /**
     * If spring is required, it looks for the spring application context, and calls createBean().  If not, the class
     * is instantiated with its default constructor.
     *
     * @param clazz The class to autowire The class to create
     * @param autowireStrategy The autowire strategy to use The strategy to use, only respected if spring is available
     * @param <T> The class type
     * @return The autowired instance
     * @throws IllegalPluginStateException If spring is required but not available
     */
    public <T> T autowire(final Class<T> clazz, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException
    {
        if (requireSpring)
        {
            assertSpringContextAvailable();
            return springContextAccessor.createBean(clazz, autowireStrategy);
        }
        else
        {
            try
            {
                return clazz.newInstance();
            }
            catch (final InstantiationException e)
            {
                throw new PluginException("Unable to instantiate " + clazz, e);
            }
            catch (final IllegalAccessException e)
            {
                throw new PluginException("Unable to access " + clazz, e);
            }
        }
    }

    /**
     * If spring is required, it looks for the spring application context and calls autowire().  If not, the object
     * is untouched.
     *
     * @param instance The instance to autowire
     * @param autowireStrategy The autowire strategy to use The strategy to use, only respected if spring is available
     * @return The autowired instance
     * @throws IllegalPluginStateException If spring is required but not available
     */
    public void autowire(final Object instance, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException
    {
        // Only do anything if spring is required
        if (requireSpring)
        {
            assertSpringContextAvailable();
            springContextAccessor.createBean(instance, autowireStrategy);
        }
    }

    public Set<String> getRequiredPlugins()
    {
        final Set<String> keys = new HashSet<String>();

        // Get a set of all packages that this plugin imports
        final Set<String> imports = OsgiHeaderUtil.parseHeader((String) getBundle().getHeaders().get(Constants.IMPORT_PACKAGE)).keySet();

        // For each import, determine what bundle provides the package
        for (final String imp : imports)
        {
            // Get a list of package exports for this package
            final ExportedPackage[] exports = packageAdmin.getExportedPackages(imp);
            if (exports != null)
            {
                // For each exported package, determine if we are a consumer
                for (final ExportedPackage export : exports)
                {
                    // Get a list of bundles that consume that package
                    final Bundle[] importingBundles = export.getImportingBundles();
                    if (importingBundles != null)
                    {
                        // For each importing bundle, determine if it is us
                        for (final Bundle importingBundle : importingBundles)
                        {
                            // If we are the bundle consumer, or importer, then add the exporter as a required plugin
                            if (getBundle() == importingBundle)
                            {
                                keys.add(OsgiHeaderUtil.getPluginKey(export.getExportingBundle()));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return keys;
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
     * @throws IllegalPluginStateException if the spring context is not initialized
     */
    private void assertSpringContextAvailable() throws IllegalPluginStateException
    {
        if (springContextAccessor == null)
        {
            throw new IllegalStateException("Cannot autowire object because the Spring context is unavailable.  " +
                "Ensure your OSGi bundle contains the 'Spring-Context' header.");
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