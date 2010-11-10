package com.atlassian.plugin.main;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.BundledPluginLoader;
import com.atlassian.plugin.loaders.ClassPathPluginLoader;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Facade interface to the Atlassian Plugins framework.  See the package Javadocs for usage information.
 */
public class AtlassianPlugins
{
    private OsgiContainerManager osgiContainerManager;
    private PluginEventManager pluginEventManager;
    private DefaultPluginManager pluginManager;
    private HotDeployer hotDeployer;

    private static final Logger log = LoggerFactory.getLogger(AtlassianPlugins.class);

    /**
     * Suffix for temporary directories which will be removed on shutdown
     */
    public static final String TEMP_DIRECTORY_SUFFIX = ".tmp";

    /**
     * Constructs an instance of the plugin framework with the specified config.  No additional validation is performed
     * on the configuration, so it is recommended you use the {@link PluginsConfigurationBuilder} class to create
     * a configuration instance.
     * @param config The plugins configuration to use
     */
    public AtlassianPlugins(PluginsConfiguration config)
    {
        pluginEventManager = new DefaultPluginEventManager();

        AtomicReference<PluginAccessor> pluginManagerRef = new AtomicReference<PluginAccessor>();
        osgiContainerManager = new FelixOsgiContainerManager(
                config.getOsgiPersistentCache(),
                config.getPackageScannerConfiguration(),
                new CriticalHostComponentProvider(config.getHostComponentProvider(), pluginEventManager, pluginManagerRef),
                pluginEventManager);

        // plugin factories/deployers
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(
                config.getPluginDescriptorFilename(),
                config.getApplicationKey(),
                config.getOsgiPersistentCache(),
                osgiContainerManager,
                pluginEventManager);
        final OsgiBundleFactory osgiBundleDeployer = new OsgiBundleFactory(osgiContainerManager, pluginEventManager);
        final List<PluginFactory> pluginDeployers = new LinkedList<PluginFactory>(Arrays.asList(osgiPluginDeployer, osgiBundleDeployer));
        if (config.isUseLegacyDynamicPluginDeployer())
        {
            pluginDeployers.add(new LegacyDynamicPluginFactory(config.getPluginDescriptorFilename()));
        }

        final List<PluginLoader> pluginLoaders = new ArrayList<PluginLoader>();

        // classpath plugins
        pluginLoaders.add(new ClassPathPluginLoader());

        // bundled plugins
        if (config.getBundledPluginUrl() != null)
        {
            pluginLoaders.add(new BundledPluginLoader(config.getBundledPluginUrl(), config.getBundledPluginCacheDirectory(), pluginDeployers, pluginEventManager));
        }

        // osgi/v2 plugins
        pluginLoaders.add(new DirectoryPluginLoader(config.getPluginDirectory(), pluginDeployers, pluginEventManager));

        pluginManager = new DefaultPluginManager(
                config.getPluginStateStore(),
                pluginLoaders,
                config.getModuleDescriptorFactory(),
                pluginEventManager);
        pluginManagerRef.set(pluginManager);

        pluginManager.setPluginInstaller(new FilePluginInstaller(config.getPluginDirectory()));

        if (config.getHotDeployPollingPeriod() > 0)
        {
            hotDeployer = new HotDeployer(pluginManager, config.getHotDeployPollingPeriod());
        }
    }

    /**
     * Starts the plugins framework.  Will return once the plugins have all been loaded and started.  Should only be
     * called once.
     * @throws PluginParseException If there was any problems parsing any of the plugins
     */
    public void start() throws PluginParseException
    {
        pluginManager.init();
        if (hotDeployer != null && !hotDeployer.isRunning())
        {
            hotDeployer.start();
        }
    }

    /**
     * Stops the framework.
     */
    public void stop()
    {
        if (hotDeployer != null && hotDeployer.isRunning())
        {
            hotDeployer.stop();
        }
        pluginManager.shutdown();
    }

    /**
     * @return the underlying OSGi container manager
     */
    public OsgiContainerManager getOsgiContainerManager()
    {
        return osgiContainerManager;
    }

    /**
     * @return the plugin event manager
     */
    public PluginEventManager getPluginEventManager()
    {
        return pluginEventManager;
    }

    /**
     * @return the plugin controller for manipulating plugins
     */
    public PluginController getPluginController()
    {
        return pluginManager;
    }

    /**
     * @return the plugin accessor for accessing plugins
     */
    public PluginAccessor getPluginAccessor()
    {
        return pluginManager;
    }

    private static class CriticalHostComponentProvider implements HostComponentProvider
    {
        private final HostComponentProvider delegate;
        private final PluginEventManager pluginEventManager;
        private final AtomicReference<PluginAccessor> pluginManagerRef;

        public CriticalHostComponentProvider(HostComponentProvider delegate, PluginEventManager pluginEventManager, AtomicReference<PluginAccessor> pluginManagerRef)
        {
            this.delegate = delegate;
            this.pluginEventManager = pluginEventManager;
            this.pluginManagerRef = pluginManagerRef;
        }

        public void provide(ComponentRegistrar registrar)
        {
            registrar.register(PluginEventManager.class).forInstance(pluginEventManager);
            registrar.register(PluginAccessor.class).forInstance(pluginManagerRef.get());
            delegate.provide(registrar);
        }
    }
}
