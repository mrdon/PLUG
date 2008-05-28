package com.atlassian.plugin.loaders.osgi;

import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.osgi.OsgiPlugin;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import com.atlassian.plugin.parsers.DescriptorParser;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import org.osgi.framework.*;
import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;
import static org.twdata.pkgscanner.PackageScanner.*;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.Felix;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OsgiPluginLoader extends ClassLoadingPluginLoader
{

    private static final Log log = LogFactory.getLog(OsgiPluginLoader.class);
    private BundleRegistration registration = null;
    private Felix felix = null;
    private String pluginDescriptorFileName;
    private XmlDescriptorParserFactory descriptorParserFactory;
    private File cacheDirectory;
    private List<Bundle> bundlesToAdd = new ArrayList();

    private List<String> jarIncludes = Arrays.asList("*.jar");
    private List<String> jarExcludes = Collections.EMPTY_LIST;
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "org.apache.*");
    private List<String> packageExcludes = Collections.EMPTY_LIST;

    private class BundleRegistration implements BundleActivator, BundleListener
    {

        public void start(BundleContext context) throws Exception {
            context.addBundleListener(this);
            for (Bundle bundle : context.getBundles()) {
                registerBundle(bundle);
            }
        }

        public void stop(BundleContext ctx) throws Exception {
        }

        public void bundleChanged(BundleEvent evt) {
            switch (evt.getType()) {
                case BundleEvent.INSTALLED:
                    log.info("Installed bundle " + evt.getBundle().getSymbolicName());
                    registerBundle(evt.getBundle());
                    break;
                case BundleEvent.STARTED:
                    log.info("Started bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.STOPPED:
                    log.info("Stopped bundle " + evt.getBundle().getSymbolicName());
                    break;
                case BundleEvent.UNINSTALLED:
                    log.info("Uninstalled bundle " + evt.getBundle().getSymbolicName());
                    unregisterBundle(evt.getBundle());
                    break;
            }
            if (evt.getType() == BundleEvent.STARTED && evt.getBundle().getSymbolicName() != null) {
            }
        }

        private void unregisterBundle(Bundle bundle) {
            synchronized(bundlesToAdd) {
                bundlesToAdd.remove(bundle);
            }

        }

        private synchronized void registerBundle(Bundle bundle) {
            synchronized(bundlesToAdd) {
                bundlesToAdd.add(bundle);
            }
        }
    }

    public OsgiPluginLoader(File cacheDirectory, String pluginDescriptorFileName)
    {
        log.debug("Creating classloader for url " + path);
        scanner = new Scanner(path);
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
        this.cacheDirectory = cacheDirectory;
    }

    synchronized void startOsgi()
    {
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

    public Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (felix == null) {
            startOsgi();
        }
        return emptyAddedList(moduleDescriptorFactory);
    }

    public boolean supportsAddition()
    {
        return true;
    }

    public boolean supportsRemoval()
    {
        return true;
    }

    public Collection addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        return emptyAddedList(moduleDescriptorFactory);
    }

    public void removePlugin(Plugin plugin) throws PluginException
    {
        super.removePlugin(plugin);
        if (plugin instanceof OsgiPlugin) {
            OsgiPlugin oplugin = (OsgiPlugin) plugin;
            try
            {
                oplugin.getBundle().uninstall();
            } catch (BundleException e)
            {
                throw new PluginException("Unable to uninstall plugin "+plugin.getKey(), e);
            }
        }
    }

    private Collection<Plugin> emptyAddedList(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Collection<Plugin> added = new ArrayList<Plugin>();
        synchronized (bundlesToAdd) {
            for (Bundle bundle : bundlesToAdd) {
                InputStream pluginDescriptor = null;
                try {
                    pluginDescriptor = bundle.getResource(pluginDescriptorFileName).openStream();
                    // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
                    DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
                    added.add(parser.configurePlugin(moduleDescriptorFactory, new OsgiPlugin(bundle)));

                } catch (IOException e)
                {
                    throw new PluginParseException("Unable to create plugin: "+bundle.getSymbolicName(), e);
                } finally {
                    try
                    {
                        pluginDescriptor.close();
                    } catch (IOException e)
                    {
                        // Ignore
                    }
                }
            }
            bundlesToAdd.clear();
        }
        return added;
    }

    private String constructAutoExports() {
        Collection<ExportPackage> exports = new PackageScanner()
           .select(
               jars(
                       include((String[]) jarIncludes.toArray()),
                       exclude((String[]) jarExcludes.toArray())),
               packages(
                       include((String[]) packageIncludes.toArray()),
                       exclude((String[]) packageExcludes.toArray())))
           .scan();

        StringBuilder sb = new StringBuilder();
        sb.append("Export-Package: ");
        for (Iterator<ExportPackage> i = exports.iterator(); i.hasNext(); ) {
            ExportPackage pkg = i.next();
            sb.append(pkg.getPackageName());
            if (pkg.getVersion() != null) {
                sb.append(";version=").append(pkg.getVersion());
            }
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
