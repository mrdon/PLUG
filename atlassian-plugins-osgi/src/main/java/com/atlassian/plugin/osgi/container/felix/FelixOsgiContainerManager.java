package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.event.events.PluginFrameworkStartingEvent;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import com.atlassian.plugin.util.ClassLoaderUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * Felix implementation of the OSGi container manager
 */
public class FelixOsgiContainerManager implements OsgiContainerManager
{
    public static final String OSGI_FRAMEWORK_BUNDLES_ZIP = "osgi-framework-bundles.zip";

    private static final Log log = LogFactory.getLog(FelixOsgiContainerManager.class);
    private static final String OSGI_BOOTDELEGATION = "org.osgi.framework.bootdelegation";
    private static final String ATLASSIAN_PREFIX = "atlassian.";

    private final File cacheDirectory;
    private final URL frameworkBundlesUrl;
    private final PackageScannerConfiguration packageScannerConfig;
    private final File frameworkBundlesDir;
    private final HostComponentProvider hostComponentProvider;
    private final Set<ServiceTracker> trackers;
    private final ExportsBuilder exportsBuilder;
    private final ThreadFactory threadFactory = new ThreadFactory()
    {
        public Thread newThread(final Runnable r)
        {
            final Thread thread = new Thread(r, "Felix:Startup");
            thread.setDaemon(true);
            return thread;
        }
    };

    private BundleRegistration registration = null;
    private Felix felix = null;
    private boolean felixRunning = false;
    private boolean disableMultipleBundleVersions = true;
    private Logger felixLogger;

    /**
     * Constructs the container manager using the framework bundles zip file located in this library
     * @param frameworkBundlesDir The directory to unzip the framework bundles into.
     * @param packageScannerConfig The configuration for package scanning
     * @param provider The host component provider.  May be null.
     * @param eventManager The plugin event manager to register for init and shutdown events
     * @deprecated Since 2.2.0, use
     *   {@link #FelixOsgiContainerManager(File,PackageScannerConfiguration,HostComponentProvider,PluginEventManager,File)} instead
     */
    @Deprecated
    public FelixOsgiContainerManager(final File frameworkBundlesDir, final PackageScannerConfiguration packageScannerConfig, final HostComponentProvider provider, final PluginEventManager eventManager)
    {
        this(ClassLoaderUtils.getResource(OSGI_FRAMEWORK_BUNDLES_ZIP, FelixOsgiContainerManager.class), frameworkBundlesDir, packageScannerConfig,
            provider, eventManager);
    }

    /**
     * Constructs the container manager
     * @param frameworkBundlesZip The location of the zip file containing framework bundles
     * @param frameworkBundlesDir The directory to unzip the framework bundles into.
     * @param packageScannerConfig The configuration for package scanning
     * @param provider The host component provider.  May be null.
     * @param eventManager The plugin event manager to register for init and shutdown events
     * @deprecated Since 2.2.0, use
     *   {@link #FelixOsgiContainerManager(URL, File,PackageScannerConfiguration,HostComponentProvider,PluginEventManager,File)} instead
     */
    @Deprecated
    public FelixOsgiContainerManager(final URL frameworkBundlesZip, final File frameworkBundlesDir, final PackageScannerConfiguration packageScannerConfig, final HostComponentProvider provider, final PluginEventManager eventManager)
    {
        this(frameworkBundlesZip, frameworkBundlesDir, packageScannerConfig, provider, eventManager, new File(frameworkBundlesDir.getParentFile(),
            "osgi-cache"));
    }

    /**
     * Constructs the container manager using the framework bundles zip file located in this library
     * @param frameworkBundlesDir The directory to unzip the framework bundles into.
     * @param packageScannerConfig The configuration for package scanning
     * @param provider The host component provider.  May be null.
     * @param eventManager The plugin event manager to register for init and shutdown events
     * @param cacheDir The directory to use for the felix cache
     *
     * @since 2.2.0
     */
    public FelixOsgiContainerManager(final File frameworkBundlesDir, final PackageScannerConfiguration packageScannerConfig, final HostComponentProvider provider, final PluginEventManager eventManager, final File cacheDir)
    {
        this(ClassLoaderUtils.getResource(OSGI_FRAMEWORK_BUNDLES_ZIP, FelixOsgiContainerManager.class), frameworkBundlesDir, packageScannerConfig,
            provider, eventManager, cacheDir);
    }

    /**
     * Constructs the container manager
     * @param frameworkBundlesZip The location of the zip file containing framework bundles
     * @param frameworkBundlesDir The directory to unzip the framework bundles into.
     * @param packageScannerConfig The configuration for package scanning
     * @param provider The host component provider.  May be null.
     * @param eventManager The plugin event manager to register for init and shutdown events
     * @param cacheDir The directory to use for the felix cache
     *
     * @since 2.2.0
     * @throws com.atlassian.plugin.osgi.container.OsgiContainerException If the host version isn't supplied and the
     *  cache directory cannot be cleaned.
     */
    public FelixOsgiContainerManager(final URL frameworkBundlesZip, final File frameworkBundlesDir,
                                     final PackageScannerConfiguration packageScannerConfig,
                                     final HostComponentProvider provider, final PluginEventManager eventManager,
                                     final File cacheDir) throws OsgiContainerException
    {
        Validate.notNull(frameworkBundlesZip, "The framework bundles zip is required");
        Validate.notNull(frameworkBundlesDir, "The framework bundles directory must not be null");
        Validate.notNull(packageScannerConfig, "The package scanner configuration must not be null");
        Validate.notNull(eventManager, "The plugin event manager is required");

        frameworkBundlesUrl = frameworkBundlesZip;
        this.packageScannerConfig = packageScannerConfig;
        this.frameworkBundlesDir = frameworkBundlesDir;
        hostComponentProvider = provider;
        trackers = Collections.synchronizedSet(new HashSet<ServiceTracker>());
        eventManager.register(this);
        felixLogger = new FelixLoggerBridge(log);
        exportsBuilder = new ExportsBuilder();
        cacheDirectory = cacheDir;
        if (!cacheDir.exists())
        {
            cacheDir.mkdir();
        }
        else if (packageScannerConfig.getCurrentHostVersion() == null)
        {
            try
            {
                FileUtils.cleanDirectory(cacheDir);
            }
            catch (final IOException e)
            {
                throw new OsgiContainerException("Unable to clean the cache directory: " + cacheDir, e);
            }
        }
        log.debug("Using cache directory :" + cacheDir.getAbsolutePath());
    }

    public void setFelixLogger(final Logger logger)
    {
        felixLogger = logger;
    }

    public void setDisableMultipleBundleVersions(final boolean val)
    {
        disableMultipleBundleVersions = val;
    }

    @PluginEventListener
    public void onStart(final PluginFrameworkStartingEvent event)
    {
        start();
    }

    @PluginEventListener
    public void onShutdown(final PluginFrameworkShutdownEvent event)
    {
        stop();
    }

    public void start() throws OsgiContainerException
    {
        if (isRunning())
        {
            return;
        }

        detectIncorrectOsgiVersion();

        final DefaultComponentRegistrar registrar = collectHostComponents(hostComponentProvider);
        // Create a case-insensitive configuration property map.
        final StringMap configMap = new StringMap(false);
        // Configure the Felix instance to be embedded.
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");
        // Add the bundle provided service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES, exportsBuilder.determineExports(registrar.getRegistry(), packageScannerConfig,
            cacheDirectory));

        // Explicitly specify the directory to use for caching bundles.
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cacheDirectory.getAbsolutePath());

        configMap.put(FelixConstants.LOG_LEVEL_PROP, String.valueOf(felixLogger.getLogLevel()));
        String bootDelegation = getAtlassianSpecificOsgiSystemProperty(OSGI_BOOTDELEGATION);
        if ((bootDelegation == null) || (bootDelegation.trim().length() == 0))
        {
            bootDelegation = "weblogic.*,META-INF.services,com.yourkit.*,com.jprofiler.*,org.apache.xerces.*";
        }

        configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, bootDelegation);
        if (log.isDebugEnabled())
        {
            log.debug("Felix configuration: " + configMap);
        }

        try
        {
            // Create host activator;
            registration = new BundleRegistration(frameworkBundlesUrl, frameworkBundlesDir, registrar);
            final List<BundleActivator> list = new ArrayList<BundleActivator>();
            list.add(registration);

            // Now create an instance of the framework with
            // our configuration properties and activator.
            felix = new Felix(felixLogger, configMap, list);

            // Now start Felix instance.  Starting in a different thread to explicity set daemon status
            final Runnable start = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        felix.start();
                        felixRunning = true;
                    }
                    catch (final BundleException e)
                    {
                        throw new OsgiContainerException("Unable to start felix", e);
                    }
                }
            };
            final Thread t = threadFactory.newThread(start);
            t.start();

            // Give it 10 seconds
            t.join(10 * 60 * 1000);
        }
        catch (final Exception ex)
        {
            throw new OsgiContainerException("Unable to start OSGi container", ex);
        }
    }

    /**
     * Detects incorrect configuration of WebSphere 6.1 that leaks OSGi 4.0 jars into the application
     */
    private void detectIncorrectOsgiVersion()
    {
        try
        {
            Bundle.class.getMethod("getBundleContext");
        }
        catch (final NoSuchMethodException e)
        {
            throw new OsgiContainerException(
                "Detected older version (4.0 or earlier) of OSGi.  If using WebSphere " + "6.1, please enable application-first (parent-last) classloading and the 'Single classloader for " + "application' WAR classloader policy.");
        }
    }

    public void stop() throws OsgiContainerException
    {
        if (felixRunning)
        {
            for (final ServiceTracker tracker : new HashSet<ServiceTracker>(trackers))
            {
                tracker.close();
            }
            felix.stopAndWait();
        }

        felixRunning = false;
        felix = null;
    }

    public Bundle[] getBundles()
    {
        if (isRunning())
        {
            return registration.getBundles();
        }
        else
        {
            throw new IllegalStateException(
                "Cannot retrieve the bundles if the Felix container isn't running. Check earlier in the logs for the possible cause as to why Felix didn't start correctly.");
        }
    }

    public ServiceReference[] getRegisteredServices()
    {
        return felix.getRegisteredServices();
    }

    public ServiceTracker getServiceTracker(final String cls)
    {
        if (!isRunning())
        {
            throw new IllegalStateException("Unable to create a tracker when osgi is not running");
        }

        final ServiceTracker tracker = registration.getServiceTracker(cls, trackers);
        tracker.open();
        trackers.add(tracker);
        return tracker;
    }

    public Bundle installBundle(final File file) throws OsgiContainerException
    {
        try
        {
            return registration.install(file, disableMultipleBundleVersions);
        }
        catch (final BundleException e)
        {
            throw new OsgiContainerException("Unable to install bundle", e);
        }
    }

    DefaultComponentRegistrar collectHostComponents(final HostComponentProvider provider)
    {
        final DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
        if (provider != null)
        {
            provider.provide(registrar);
        }
        return registrar;
    }

    public boolean isRunning()
    {
        return felixRunning;
    }

    public List<HostComponentRegistration> getHostComponentRegistrations()
    {
        return registration.getHostComponentRegistrations();
    }

    private String getAtlassianSpecificOsgiSystemProperty(final String originalSystemProperty)
    {
        return System.getProperty(ATLASSIAN_PREFIX + originalSystemProperty);
    }

    /**
     * Manages framework-level framework bundles and host components registration, and individual plugin bundle
     * installation and removal.
     */
    static class BundleRegistration implements BundleActivator, BundleListener
    {
        private BundleContext bundleContext;
        private final DefaultComponentRegistrar registrar;
        private List<ServiceRegistration> hostServicesReferences;
        private List<HostComponentRegistration> hostComponentRegistrations;
        private final URL frameworkBundlesUrl;
        private final File frameworkBundlesDir;
        private PackageAdmin packageAdmin;

        public BundleRegistration(final URL frameworkBundlesUrl, final File frameworkBundlesDir, final DefaultComponentRegistrar registrar)
        {
            this.registrar = registrar;
            this.frameworkBundlesUrl = frameworkBundlesUrl;
            this.frameworkBundlesDir = frameworkBundlesDir;
        }

        public void start(final BundleContext context) throws Exception
        {
            bundleContext = context;
            final ServiceReference ref = context.getServiceReference(org.osgi.service.packageadmin.PackageAdmin.class.getName());
            packageAdmin = (PackageAdmin) context.getService(ref);

            context.addBundleListener(this);

            loadHostComponents(registrar);
            extractAndInstallFrameworkBundles();
            context.addFrameworkListener(new FrameworkListener()
            {
                public void frameworkEvent(final FrameworkEvent event)
                {
                    String bundleBits = "";
                    if (event.getBundle() != null)
                    {
                        bundleBits = " in bundle " + event.getBundle().getSymbolicName();
                    }
                    switch (event.getType())
                    {
                        case FrameworkEvent.ERROR:
                            log.error("Framework error" + bundleBits, event.getThrowable());
                            break;
                        case FrameworkEvent.WARNING:
                            log.warn("Framework warning" + bundleBits, event.getThrowable());
                            break;
                        case FrameworkEvent.INFO:
                            log.info("Framework info" + bundleBits, event.getThrowable());
                            break;
                    }
                }
            });
        }

        public void stop(final BundleContext ctx) throws Exception
        {}

        public void bundleChanged(final BundleEvent evt)
        {
            switch (evt.getType())
            {
                case BundleEvent.INSTALLED:
                    log.info("Installed bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
                case BundleEvent.RESOLVED:
                    log.info("Resolved bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
                case BundleEvent.UNRESOLVED:
                    log.info("Unresolved bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
                case BundleEvent.STARTED:
                    log.info("Started bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
                case BundleEvent.STOPPED:
                    log.info("Stopped bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
                case BundleEvent.UNINSTALLED:
                    log.info("Uninstalled bundle " + evt.getBundle().getSymbolicName() + " (" + evt.getBundle().getBundleId() + ")");
                    break;
            }
        }

        public Bundle install(final File path, final boolean uninstallOtherVersions) throws BundleException
        {
            boolean bundleUninstalled = false;
            if (uninstallOtherVersions)
            {
                try
                {
                    final JarFile jar = new JarFile(path);
                    final String name = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                    for (final Bundle oldBundle : bundleContext.getBundles())
                    {
                        if (name.equals(oldBundle.getSymbolicName()))
                        {
                            log.info("Uninstalling existing version " + oldBundle.getHeaders().get(Constants.BUNDLE_VERSION));
                            oldBundle.uninstall();
                            bundleUninstalled = true;
                        }
                    }
                }
                catch (final IOException e)
                {
                    throw new BundleException("Invalid bundle format", e);
                }
            }
            final Bundle bundle = bundleContext.installBundle(path.toURI().toString());
            if (bundleUninstalled)
            {
                refreshPackages();
            }
            return bundle;
        }

        public Bundle[] getBundles()
        {
            return bundleContext.getBundles();
        }

        public ServiceTracker getServiceTracker(final String clazz, final Set<ServiceTracker> trackedTrackers)
        {
            return new ServiceTracker(bundleContext, clazz, null)
            {
                @Override
                public void close()
                {
                    trackedTrackers.remove(this);
                }
            };
        }

        public List<HostComponentRegistration> getHostComponentRegistrations()
        {
            return hostComponentRegistrations;
        }

        private void loadHostComponents(final DefaultComponentRegistrar registrar)
        {
            // Unregister any existing host components
            if (hostServicesReferences != null)
            {
                for (final ServiceRegistration reg : hostServicesReferences)
                {
                    reg.unregister();
                }
            }

            // Register host components as OSGi services
            hostServicesReferences = registrar.writeRegistry(bundleContext);
            hostComponentRegistrations = registrar.getRegistry();
        }

        private void extractAndInstallFrameworkBundles() throws BundleException
        {
            final List<Bundle> bundles = new ArrayList<Bundle>();
            com.atlassian.plugin.util.FileUtils.conditionallyExtractZipFile(frameworkBundlesUrl, frameworkBundlesDir);
            for (final File bundleFile : frameworkBundlesDir.listFiles(new FilenameFilter()
                {
                    public boolean accept(final File file, final String s)
                    {
                        return s.endsWith(".jar");
                    }
                }))
            {
                bundles.add(install(bundleFile, false));
            }

            for (final Bundle bundle : bundles)
            {
                bundle.start();
            }
        }

        private void refreshPackages()
        {
            final CountDownLatch latch = new CountDownLatch(1);
            FrameworkListener refreshListener = new FrameworkListener()
            {

                public void frameworkEvent(FrameworkEvent event)
                {
                    if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
                    {
                        log.info("Packages refreshed");
                        latch.countDown();
                    }
                }
            };
            bundleContext.addFrameworkListener(refreshListener);
            packageAdmin.refreshPackages(null);
            boolean refreshed = false;
            try
            {
                refreshed = latch.await(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
            if (!refreshed)
            {
                log.warn("Timeout exceeded waiting for package refresh");
            }
            bundleContext.removeFrameworkListener(refreshListener);
        }
    }

}
