package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.impl.DefaultComponentRegistrar;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.parsers.DescriptorParser;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoActivator;
import org.osgi.framework.*;
import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;
import static org.twdata.pkgscanner.PackageScanner.jars;
import static org.twdata.pkgscanner.PackageScanner.include;
import static org.twdata.pkgscanner.PackageScanner.exclude;
import static org.twdata.pkgscanner.PackageScanner.packages;

public class OsgiPluginLoader extends ClassLoadingPluginLoader
{
    private static final Log log = LogFactory.getLog(OsgiPluginLoader.class);
    private BundleRegistration registration = null;
    private Felix felix = null;
    private File cacheDirectory;

    private List<String> jarIncludes = Arrays.asList("*.jar");
    private List<String> jarExcludes = Collections.EMPTY_LIST;
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "org.apache.*", "org.xml.*", "javax.*", "org.w3c.*");
    private List<String> packageExcludes = Collections.EMPTY_LIST;
    private HostComponentProvider hostComponentProvider;
    private File startBundlesPath;

    public OsgiPluginLoader(File pluginPath, File startBundlesPath, PluginFactory pluginFactory, HostComponentProvider provider)
    {
        super(pluginPath, pluginFactory);
        this.hostComponentProvider = provider;
        this.startBundlesPath = startBundlesPath;
    }

    public OsgiPluginLoader(File pluginPath, File startBundlesPath, String pluginDescriptorFileName, PluginFactory pluginFactory, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory);
        this.hostComponentProvider = provider;
        this.startBundlesPath = startBundlesPath;
    }

    /**
     * Forces all registered host components to be unregistered and the HostComponentProvider to be called to get a
     * new list of host components
     */
    public void reloadHostComponents()
    {
        registration.reloadHostComponents();
    }

    @Override
    public void shutDown()
    {
        super.shutDown();
        try
        {
            felix.stop();
        } catch (BundleException e)
        {
            throw new RuntimeException("Unable to stop OSGi container", e);
        }
    }

    @Override
    public synchronized Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
    {
        if (felix == null)
        {
            startOsgi(startBundlesPath, hostComponentProvider);
        }
        return super.loadAllPlugins(moduleDescriptorFactory);
    }

    @Override
    protected Plugin createPlugin(DescriptorParser parser, DeploymentUnit unit, PluginClassLoader loader) {
        Plugin plugin = null;
        switch (parser.getPluginsVersion()) {
            case 2  : plugin = createOsgiPlugin(unit.getPath(), false);
                      break;
            default : plugin = super.createPlugin(parser, unit, loader);
        }
        return plugin;
    }

    @Override
    protected Plugin handleNoDescriptor(DeploymentUnit deploymentUnit) throws PluginParseException
    {
        try
        {
            JarFile jar = new JarFile(deploymentUnit.getPath());
            Attributes attrs = jar.getManifest().getMainAttributes();
            String name = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
            if (name != null) {
                return createOsgiPlugin(deploymentUnit.getPath(), true);
            }
        } catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);
    }

    synchronized void startOsgi(File startBundlesPath, HostComponentProvider provider)
    {
        try
        {
            cacheDirectory = new File(File.createTempFile("foo", "bar").getParentFile(), "felix");
            if (cacheDirectory.exists())
                deleteDirectory(cacheDirectory);

            cacheDirectory.mkdir();
            cacheDirectory.deleteOnExit();
        } catch (IOException e)
        {
            throw new RuntimeException("Cannot create cache directory", e);
        }

        // Create a case-insensitive configuration property map.
        final Map configMap = new StringMap(false);
        // Configure the Felix instance to be embedded.
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");
        // Add the bundle provided service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.3.0," +
            "org.osgi.service.packageadmin; version=1.2.0," +
            "org.osgi.service.startlevel; version=1.0.0," +
            "org.osgi.service.url; version=1.0.0," +
            "org.osgi.util; version=1.3.0," +
            "org.osgi.util.tracker; version=1.3.0," +
            "host.service.command; version=1.0.0," +

            constructAutoExports());
        configMap.put(AutoActivator.AUTO_START_PROP + ".1", constructStartBundles(startBundlesPath));

        // Ensure bundles start at level 2 so that our system bundles have already been loaded
        //configMap.put(FelixConstants.FRAMEWORK_STARTLEVEL_PROP, "2");
        //configMap.put(FelixConstants.BUNDLE_STARTLEVEL_PROP, "2");

        configMap.put(FelixConstants.LOG_LEVEL_PROP, "4");
        // Explicitly specify the directory to use for caching bundles.
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cacheDirectory.getAbsolutePath());

        try
        {
            // Create host activator;
            registration = new BundleRegistration(startBundlesPath, provider);
            final List list = new ArrayList();
            list.add(registration);

            // Now create an instance of the framework with
            // our configuration properties and activator.
            felix = new Felix(configMap, list);

            // Now start Felix instance.  Starting in a different thread to explicity set daemon status
            Thread t = new Thread() {
                @Override
                public void run() {
                    try
                    {

                        felix.start();
                    } catch (BundleException e)
                    {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            };
            t.setDaemon(true);
            t.start();

            // Give it 10 seconds
            t.join(10 * 60 * 1000);



        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
        }
    }

    public void setJarPatterns(List<String> includes, List<String> excludes) {
        this.jarIncludes = includes;
        this.jarExcludes = excludes;
    }

    public void setPackagePatterns(List<String> includes, List<String> excludes) {
        this.packageIncludes = includes;
        this.packageExcludes = excludes;
    }

    private String constructAutoExports() {
        Collection<ExportPackage> exports = new PackageScanner()
           .select(
               jars(
                       include((String[]) jarIncludes.toArray(new String[0])),
                       exclude((String[]) jarExcludes.toArray(new String[0]))),
               packages(
                       include((String[]) packageIncludes.toArray(new String[0])),
                       exclude((String[]) packageExcludes.toArray(new String[0]))))
           .scan();

        StringBuilder sb = new StringBuilder();
        for (Iterator<ExportPackage> i = exports.iterator(); i.hasNext(); ) {
            ExportPackage pkg = i.next();
            sb.append(pkg.getPackageName());
            if (pkg.getVersion() != null && !pkg.getVersion().contains("SNAPSHOT")) {
                try {
                    Version.parseVersion(pkg.getVersion());
                    sb.append(";version=").append(pkg.getVersion());
                } catch (IllegalArgumentException ex) {
                    log.info("Unable to parse version: "+pkg.getVersion());
                }
            }
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private String constructStartBundles(File parent) {
        StringBuilder startBundles = new StringBuilder();
        try
            {
            for (File bundleFile : parent.listFiles(new FilenameFilter() {
                public boolean accept(File file, String s) {return s.endsWith(".jar");}}))
            {
                startBundles.append(bundleFile.toURL().toString()).append(" ");
            }
        } catch (MalformedURLException e)
        {
            // Should never happen
            throw new RuntimeException("Invalid started bundle URL", e);
        }
        if (startBundles.length() > 0) {
            startBundles.deleteCharAt(startBundles.length() - 1);
        }
        return startBundles.toString();
    }

    static private boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
    return( path.delete() );
    }


    private Plugin createOsgiPlugin(File file, boolean bundle)
    {
        try
        {
            if (bundle)
                return new OsgiBundlePlugin(registration.install(file));
            else
                return new OsgiPlugin(registration.install(file));
        } catch (BundleException e)
        {
            log.error("Unable to load plugin: "+file, e);
            
            UnloadablePlugin plugin = new UnloadablePlugin();
            plugin.setErrorText("Unable to load plugin: "+e.getMessage());
            return plugin;
        }
    }

    private static class BundleRegistration implements BundleActivator, BundleListener
    {
        private BundleContext bundleContext;
        private HostComponentProvider hostProvider;
        private File startBundlesPath;
        private List<ServiceRegistration> hostServices;

        public BundleRegistration(File startBundlesPath, HostComponentProvider provider)
        {
            this.startBundlesPath = startBundlesPath;
            this.hostProvider = provider;
        }

        public void start(BundleContext context) throws Exception {
            this.bundleContext = context;
            context.addBundleListener(this);

            reloadHostComponents();

            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/aopalliance.osgi-1.0-SNAPSHOT.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/slf4j-api-1.4.3.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/log4j.osgi-1.2.15-SNAPSHOT.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/slf4j-log4j12-1.4.3.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/jcl104-over-slf4j-1.4.3.jar").start();

            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-core-2.5.1.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-beans-2.5.1.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-aop-2.5.1.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-context-2.5.1.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-osgi-io-1.0.2.jar").start();
            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-osgi-core-1.0.2.jar").start();

            context.installBundle("file:/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-osgi-extender-1.0.2.jar").start();
        }

        public void reloadHostComponents()
        {
            // Unregister any existing host components
            if (hostServices != null) {
                for (ServiceRegistration reg : hostServices)
                    reg.unregister();
            }

            // Retrieve and register host components as OSGi services
            DefaultComponentRegistrar registrar = new DefaultComponentRegistrar();
            hostProvider.provide(registrar);
            hostServices = registrar.writeRegistry(bundleContext);
        }

        public void stop(BundleContext ctx) throws Exception {
        }

        public void bundleChanged(BundleEvent evt) {
            switch (evt.getType()) {
                case BundleEvent.INSTALLED:
                    log.warn("Installed bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.STARTED:
                    log.warn("Started bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.STOPPED:
                    log.warn("Stopped bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.UNINSTALLED:
                    log.warn("Uninstalled bundle " + evt.getBundle().getSymbolicName());
                    break;
            }
            if (evt.getType() == BundleEvent.STARTED && evt.getBundle().getSymbolicName() != null) {
            }
        }

        public Bundle install(File path) throws BundleException
        {
            Bundle bundle = null;
            try
            {
                
                bundle = bundleContext.installBundle(path.toURL().toString());
            } catch (MalformedURLException e)
            {
                throw new BundleException("Invalid path: "+path);
            }
            bundle.start();
            return bundle;
        }

        public void uninstall(Bundle bundle) throws BundleException
        {
            bundle.uninstall();
        }

    }

}
