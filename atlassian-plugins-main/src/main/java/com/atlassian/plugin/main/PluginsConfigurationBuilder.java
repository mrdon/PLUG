package com.atlassian.plugin.main;

import static com.atlassian.plugin.util.Assertions.isTrue;
import static com.atlassian.plugin.util.Assertions.notNull;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginStateStore;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.store.MemoryPluginStateStore;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * The builder for {@link PluginsConfiguration} instances that additionally performs validation and default creation.
 * For a usage example, see the package javadocs.
 * <p>
 * Not thread-safe. Instances of this class should be thread and preferably method local.
 * 
 * @since 2.2
 */
public class PluginsConfigurationBuilder
{
    /**
     * Static factory for creating a new builder.
     * @return a new builder.
     */
    public static PluginsConfigurationBuilder pluginsConfiguration()
    {
        return new PluginsConfigurationBuilder();
    }

    private PackageScannerConfiguration packageScannerConfiguration;
    private HostComponentProvider hostComponentProvider;
    private File frameworkBundlesDirectory;
    private File bundleCacheDirectory;
    private File pluginDirectory;
    private URL bundledPluginUrl;
    private File bundledPluginCacheDirectory;
    private String pluginDescriptorFilename;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private PluginStateStore pluginStateStore;
    private long hotDeployPollingPeriod;

    /**
     * Sets the package scanner configuration instance that contains information about what packages to expose to plugins.
     * 
     * @param packageScannerConfiguration The configuration instance
     * @return this
     */
    public PluginsConfigurationBuilder packageScannerConfiguration(final PackageScannerConfiguration packageScannerConfiguration)
    {
        this.packageScannerConfiguration = notNull("packageScannerConfiguration", packageScannerConfiguration);
        return this;
    }

    /**
     * Sets the host component provider instance, used for registering application services as OSGi services so that
     * they can be automatically available to plugins
     *
     * @param hostComponentProvider The host component provider implementation
     * @return this
     */
    public PluginsConfigurationBuilder hostComponentProvider(final HostComponentProvider hostComponentProvider)
    {
        this.hostComponentProvider = notNull("hostComponentProvider", hostComponentProvider);
        return this;
    }

    /**
     * Sets caching directory to extract framework bundles into.  Doesn't have to be preserved and will be automatically
     * cleaned out if it detects any modification.
     *
     * @param frameworkBundlesDirectory A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder frameworkBundlesDirectory(final File frameworkBundlesDirectory)
    {
        this.frameworkBundlesDirectory = frameworkBundlesDirectory;
        return this;
    }

    /**
     * Sets the directory to use for the OSGi framework's bundle cache.  Doesn't have to be preserved across restarts
     * but shouldn't be externally modified at runtime.
     *
     * @param bundleCacheDirectory A directory that exists and is empty
     * @return this
     */
    public PluginsConfigurationBuilder bundleCacheDirectory(final File bundleCacheDirectory)
    {
        this.bundleCacheDirectory = bundleCacheDirectory;
        return this;
    }

    /**
     * Sets the directory that contains the plugins and will be used to store installed plugins.
     *
     * @param pluginDirectory A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder pluginDirectory(final File pluginDirectory)
    {
        this.pluginDirectory = pluginDirectory;
        return this;
    }

    /**
     * Sets the URL to a ZIP file containing plugins that are to be started before any user plugins but after
     * framework bundles.  Must be set if {@link #bundledPluginCacheDirectory(java.io.File)} is set.
     *
     * @param bundledPluginUrl A URL to a ZIP of plugin JAR files
     * @return this
     */
    public PluginsConfigurationBuilder bundledPluginUrl(final URL bundledPluginUrl)
    {
        this.bundledPluginUrl = bundledPluginUrl;
        return this;
    }

    /**
     * Sets the directory to unzip bundled plugins into.  The directory will automatically be cleaned out if the
     * framework detects any modification.  Must be set if {@link #bundledPluginUrl(java.net.URL)} is set.
     *
     * @param bundledPluginCacheDirectory A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder bundledPluginCacheDirectory(final File bundledPluginCacheDirectory)
    {
        this.bundledPluginCacheDirectory = bundledPluginCacheDirectory;
        return this;
    }

    /**
     * Sets the plugin descriptor file name to expect in a plugin JAR artifact
     *
     * @param pluginDescriptorFilename A valid file name
     * @return this
     */
    public PluginsConfigurationBuilder pluginDescriptorFilename(final String pluginDescriptorFilename)
    {
        this.pluginDescriptorFilename = pluginDescriptorFilename;
        return this;
    }

    /**
     * Sets the module descriptor factory that will be used to create instances of discovered module descriptors.
     * Usually, the {@link DefaultModuleDescriptorFactory} is what is used, which takes class instances of module
     * descriptors to instantiate.
     *
     * @param moduleDescriptorFactory A module descriptor factory instance
     * @return this
     */
    public PluginsConfigurationBuilder moduleDescriptorFactory(final ModuleDescriptorFactory moduleDescriptorFactory)
    {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        return this;
    }

    /**
     * Sets the plugin state store implementation used for persisting which plugins and modules are enabled or disabled
     * across restarts.
     *
     * @param pluginStateStore The plugin state store implementation
     * @return this
     */
    public PluginsConfigurationBuilder pluginStateStore(final PluginStateStore pluginStateStore)
    {
        this.pluginStateStore = pluginStateStore;
        return this;
    }

    /**
     * Sets the polling frequency for scanning for new plugins
     *
     * @param hotDeployPollingFrequency The quantity of time periods
     * @param timeUnit The units for the frequency
     */
    public PluginsConfigurationBuilder hotDeployPollingFrequency(final long hotDeployPollingFrequency, final TimeUnit timeUnit)
    {
        hotDeployPollingPeriod = hotDeployPollingFrequency * timeUnit.toMillis(hotDeployPollingFrequency);
        return this;
    }

    /**
     * Builds a {@link com.atlassian.plugin.main.PluginsConfiguration} instance by processing the configuration that
     * was previously set, validating the input, and setting any defaults where not explicitly specified.
     *
     * @return A valid {@link PluginsConfiguration} instance to pass to {@link AtlassianPlugins}
     */
    public PluginsConfiguration build()
    {
        notNull("Plugin directory must be defined", pluginDirectory);
        isTrue("Plugin directory must exist", pluginDirectory.exists());

        if (packageScannerConfiguration == null)
        {
            packageScannerConfiguration = new PackageScannerConfigurationBuilder().build();
        }
        if (pluginDescriptorFilename == null)
        {
            pluginDescriptorFilename = PluginAccessor.Descriptor.FILENAME;
        }

        if (hostComponentProvider == null)
        {
            hostComponentProvider = new HostComponentProvider()
            {
                public void provide(final ComponentRegistrar registrar)
                {}
            };
        }

        if (pluginStateStore == null)
        {
            pluginStateStore = new MemoryPluginStateStore();
        }

        if (moduleDescriptorFactory == null)
        {
            moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        }

        if (bundleCacheDirectory == null)
        {
            try
            {
                bundleCacheDirectory = File.createTempFile("atlassian-plugins-bundle-cache", ".tmp");
                bundleCacheDirectory.delete();
            }
            catch (final IOException e)
            {
                throw new IllegalStateException("Should be able to create tmp files", e);
            }
            bundleCacheDirectory.mkdir();
        }
        if (!bundleCacheDirectory.exists())
        {
            throw new IllegalArgumentException("Bundle cache directory should exist");
        }

        if (frameworkBundlesDirectory == null)
        {
            try
            {
                frameworkBundlesDirectory = File.createTempFile("atlassian-plugins-framework-bundles", ".tmp");
                frameworkBundlesDirectory.delete();
            }
            catch (final IOException e)
            {
                throw new IllegalStateException("Should be able to create tmp files", e);
            }
            frameworkBundlesDirectory.mkdir();
        }
        isTrue("Framework bundles directory should exist", frameworkBundlesDirectory.exists());

        if ((bundledPluginCacheDirectory == null) ^ (bundledPluginUrl == null))
        {
            throw new IllegalArgumentException("Both bundled plugin cache directory and bundle plugin URL must be defined or not at all.");
        }

        return new InternalPluginsConfiguration(this);
    }

    private static class InternalPluginsConfiguration implements PluginsConfiguration
    {
        private final PackageScannerConfiguration packageScannerConfiguration;
        private final HostComponentProvider hostComponentProvider;
        private final File frameworkBundlesDirectory;
        private final File bundleCacheDirectory;
        private final File pluginDirectory;
        private final URL bundledPluginUrl;
        private final File bundledPluginCacheDirectory;
        private final String pluginDescriptorFilename;
        private final ModuleDescriptorFactory moduleDescriptorFactory;
        private final PluginStateStore pluginStateStore;
        private final long hotDeployPollingPeriod;

        InternalPluginsConfiguration(final PluginsConfigurationBuilder builder)
        {
            packageScannerConfiguration = builder.packageScannerConfiguration;
            hostComponentProvider = builder.hostComponentProvider;
            frameworkBundlesDirectory = builder.frameworkBundlesDirectory;
            bundleCacheDirectory = builder.bundleCacheDirectory;
            pluginDirectory = builder.pluginDirectory;
            bundledPluginUrl = builder.bundledPluginUrl;
            bundledPluginCacheDirectory = builder.bundledPluginCacheDirectory;
            pluginDescriptorFilename = builder.pluginDescriptorFilename;
            moduleDescriptorFactory = builder.moduleDescriptorFactory;
            pluginStateStore = builder.pluginStateStore;
            hotDeployPollingPeriod = builder.hotDeployPollingPeriod;
        }

        public PackageScannerConfiguration getPackageScannerConfiguration()
        {
            return packageScannerConfiguration;
        }

        public HostComponentProvider getHostComponentProvider()
        {
            return hostComponentProvider;
        }

        public File getFrameworkBundlesDirectory()
        {
            return frameworkBundlesDirectory;
        }

        public File getBundleCacheDirectory()
        {
            return bundleCacheDirectory;
        }

        public String getPluginDescriptorFilename()
        {
            return pluginDescriptorFilename;
        }

        public File getPluginDirectory()
        {
            return pluginDirectory;
        }

        public URL getBundledPluginUrl()
        {
            return bundledPluginUrl;
        }

        public File getBundledPluginCacheDirectory()
        {
            return bundledPluginCacheDirectory;
        }

        public ModuleDescriptorFactory getModuleDescriptorFactory()
        {
            return moduleDescriptorFactory;
        }

        public PluginStateStore getPluginStateStore()
        {
            return pluginStateStore;
        }

        public long getHotDeployPollingPeriod()
        {
            return hotDeployPollingPeriod;
        }
    }
}
