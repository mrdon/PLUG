package com.atlassian.plugin.loaders.classloading.osgi;

import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.parsers.DescriptorParser;

import java.io.File;
import java.io.IOException;
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
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "org.apache.*");
    private List<String> packageExcludes = Collections.EMPTY_LIST;

    public OsgiPluginLoader(File path, PluginFactory pluginFactory)
    {
        super(path, pluginFactory);
        startOsgi();
    }

    public OsgiPluginLoader(File path, String pluginDescriptorFileName, PluginFactory pluginFactory)
    {
        super(path, pluginDescriptorFileName, pluginFactory);
        startOsgi();
    }

    /**
     * @param plugin - the plugin to remove
     * @throws com.atlassian.plugin.PluginException representing the reason for failure.
     */
    @Override
    public void removePlugin(Plugin plugin) throws PluginException
    {
        if (plugin instanceof OsgiPlugin) {
            try
            {
                registration.uninstall(((OsgiPlugin)plugin).getBundle());
            } catch (BundleException e)
            {
                throw new PluginException("Unable to uninstall", e);
            }
        } else {
            super.removePlugin(plugin);
        }

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

    synchronized void startOsgi()
    {
        try
        {
            cacheDirectory = new File(File.createTempFile("foo", "bar").getParentFile(), "felix");
            cacheDirectory.mkdir();
            cacheDirectory.deleteOnExit();
        } catch (IOException e)
        {
            throw new RuntimeException("Cannot create cache directory", e);
        }

        // Create a case-insensitive configuration property map.
        Map configMap = new StringMap(false);
        // Configure the Felix instance to be embedded.
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");
        // Add the bundle provided service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.3.0," +
            "org.osgi.service.packageadmin; version=1.2.0," +
            "org.osgi.service.startlevel; version=1.0.0," +
            "org.osgi.service.url; version=1.0.0," +
            "host.service.command; version=1.0.0," +
            constructAutoExports());
        // Explicitly specify the directory to use for caching bundles.
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cacheDirectory.getAbsolutePath());

        try
        {
            // Create host activator;
            registration = new BundleRegistration();
            List list = new ArrayList();
            list.add(registration);

            // Now create an instance of the framework with
            // our configuration properties and activator.
            felix = new Felix(configMap, list);

            // Now start Felix instance.
            felix.start();
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

        public void start(BundleContext context) throws Exception {
            context.addBundleListener(this);
            this.bundleContext = context;
        }

        public void stop(BundleContext ctx) throws Exception {
        }

        public void bundleChanged(BundleEvent evt) {
            switch (evt.getType()) {
                case BundleEvent.INSTALLED:
                    log.info("Installed bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.STARTED:
                    log.info("Started bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.STOPPED:
                    log.info("Stopped bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.UNINSTALLED:
                    log.info("Uninstalled bundle " + evt.getBundle().getSymbolicName());
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
