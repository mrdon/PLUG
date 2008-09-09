package com.atlassian.plugin.osgi;

import junit.framework.TestCase;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.*;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

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
    private PluginEventManager pluginEventManager;

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
        if (osgiContainerManager != null) {
            osgiContainerManager.stop();
        }
        FileUtils.deleteDirectory(frameworkBundlesDir);
        FileUtils.deleteDirectory(pluginsDir);
        osgiContainerManager = null;
        tmpDir = null;
        frameworkBundlesDir = null;
        pluginsDir = null;
        moduleDescriptorFactory = null;
        pluginManager = null;
        pluginEventManager = null;
    }

    protected void initPluginManager(HostComponentProvider hostComponentProvider) throws Exception {
        PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        scannerConfig.getPackageIncludes().add("com.atlassian.plugin*");
        pluginEventManager = new DefaultPluginEventManager();
        osgiContainerManager = new FelixOsgiContainerManager(frameworkBundlesDir,
                                                             scannerConfig, hostComponentProvider, pluginEventManager);

        OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, osgiContainerManager);

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Collections.<PluginFactory>singletonList(osgiPluginDeployer), new DefaultPluginEventManager());

        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader>asList(loader),
                moduleDescriptorFactory, pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }
}
