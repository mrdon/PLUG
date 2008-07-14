package com.atlassian.plugin.osgi;

import junit.framework.TestCase;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.loader.OsgiPluginLoader;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.*;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.plugin.loaders.DefaultPluginFactory;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

/**
 * Base for in-container unit tests
 */
public abstract class PluginInContainerTestBase extends TestCase {
    protected OsgiContainerManager osgiContainerManager;
    protected File tmpDir;
    protected File frameworkBundlesDir;
    protected File pluginsDir;
    protected ModuleDescriptorFactory moduleDescriptorFactory;
    protected DefaultPluginManager pluginManager;

    @Override
    public void setUp() throws Exception
    {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
        frameworkBundlesDir = new File(tmpDir, "framework-bundles");
        frameworkBundlesDir.mkdir();
        pluginsDir = new File(tmpDir, "plugins");
        pluginsDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(frameworkBundlesDir);
        FileUtils.deleteDirectory(pluginsDir);
        osgiContainerManager = null;
        tmpDir = null;
        frameworkBundlesDir = null;
        pluginsDir = null;
        moduleDescriptorFactory = null;
        pluginManager = null;
    }

    protected void initPluginManager(HostComponentProvider hostComponentProvider) throws Exception {
        PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        scannerConfig.getPackageIncludes().add("com.atlassian.plugin*");
        osgiContainerManager = new FelixOsgiContainerManager(frameworkBundlesDir,
                                                             scannerConfig);

        OsgiPluginLoader osgiPluginLoader = new OsgiPluginLoader(
                pluginsDir,
                PluginManager.PLUGIN_DESCRIPTOR_FILENAME,
                new DefaultPluginFactory(),
                osgiContainerManager,
                hostComponentProvider);
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.asList(osgiPluginLoader),
                moduleDescriptorFactory);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }
}
