package com.atlassian.plugin.main;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginStateStore;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;

import java.io.File;
import java.net.URL;

/**
 * Configuration for the Atlassian Plugins Framework.  Instances of this class should be created via the
 * {@link PluginsConfigurationBuilder}.
 */
public interface PluginsConfiguration
{
    /**
     * @return The package scanner configuration
     */
    PackageScannerConfiguration getPackageScannerConfiguration();

    /**
     * @return the host component provider
     */
    HostComponentProvider getHostComponentProvider();

    /**
     * @return the framework bundles directory
     */
    File getFrameworkBundlesDirectory();

    /**
     * @return the directory to use for the osgi framework bundles cache
     */
    File getBundleCacheDirectory();

    /**
     * @return the name of the plugin descriptor file
     */
    String getPluginDescriptorFilename();

    /**
     * @return the directory containing plugins
     */
    File getPluginDirectory();

    /**
     * @return the location of the bundled plugins zip
     */
    URL getBundledPluginUrl();

    /**
     * @return the directory to unzip the bundled plugins into
     */
    File getBundledPluginCacheDirectory();

    /**
     * @return the factory for module descriptors
     */
    ModuleDescriptorFactory getModuleDescriptorFactory();

    /**
     * @return the plugin state store implementation
     */
    PluginStateStore getPluginStateStore();
}
