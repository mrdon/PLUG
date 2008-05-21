package com.atlassian.plugin.osgi;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.InputStream;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.predicate.PluginPredicate;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;

/**
 * OSGI Plugin Accessor
 */
public class OsgiPluginAccessor implements PluginAccessor
{
    private static final Log log = LogFactory.getLog(OsgiPluginAccessor.class);
    private BundleRegistration registration = null;
    private Felix felix = null;
    private Map<String,OsgiPlugin> plugins = new HashMap<String,OsgiPlugin>();

    private class BundleRegistration implements BundleActivator, BundleListener {

        public void start(BundleContext context) throws Exception {
            ServiceTracker tracker = new ServiceTracker(context, new Filter() {

                public boolean match(ServiceReference serviceReference) {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public boolean match(Dictionary dictionary) {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public boolean matchCase(Dictionary dictionary) {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }
            })
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
            plugins.remove(bundle.getSymbolicName());
        }

        private void registerBundle(Bundle bundle) {
            plugins.put(bundle.getSymbolicName(), new OsgiPlugin(bundle));
        }
    }

    public OsgiPluginAccessor()
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
            "host.service.command; version=1.0.0");
        // Explicitly specify the directory to use for caching bundles.
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, "cache");

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

    public Collection<OsgiPlugin> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    public Collection getPlugins(PluginPredicate pluginPredicate) {
        List<Plugin> matchingPlugins = new ArrayList<Plugin>();
        for (Plugin p : getPlugins()) {
            if (pluginPredicate.matches(p)) {
                matchingPlugins.add(p);
            }
        }
        return matchingPlugins;
    }

    public Collection getEnabledPlugins() {
        return getPlugins(new PluginPredicate() {
            public boolean matches(Plugin plugin) {
                return plugin.isEnabled();
            }
        })
    }

    public Collection getModules(ModuleDescriptorPredicate moduleDescriptorPredicate) {
        List
    }

    public Collection getModuleDescriptors(ModuleDescriptorPredicate moduleDescriptorPredicate) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Plugin getPlugin(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Plugin getEnabledPlugin(String pluginKey) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ModuleDescriptor getPluginModule(String completeKey) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ModuleDescriptor getEnabledPluginModule(String completeKey) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isPluginEnabled(String key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isPluginModuleEnabled(String completeKey) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getEnabledModulesByClass(Class moduleClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getEnabledModulesByClassAndDescriptor(Class[] descriptorClazz, Class moduleClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getEnabledModulesByClassAndDescriptor(Class descriptorClazz, Class moduleClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getEnabledModuleDescriptorsByClass(Class descriptorClazz) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getEnabledModuleDescriptorsByType(String type) throws PluginParseException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getDynamicResourceAsStream(String resourcePath) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getPluginResourceAsStream(String pluginKey, String resourcePath) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class getDynamicPluginClass(String className) throws ClassNotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSystemPlugin(String key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}