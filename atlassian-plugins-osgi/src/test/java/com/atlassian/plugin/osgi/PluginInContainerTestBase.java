package com.atlassian.plugin.osgi;

import junit.framework.TestCase;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.*;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
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
    protected File cacheDir;
    protected File frameworkBundlesDir;
    protected File pluginsDir;
    protected ModuleDescriptorFactory moduleDescriptorFactory;
    protected DefaultPluginManager pluginManager;
    private PluginEventManager pluginEventManager;

    @Override
    public void setUp() throws Exception
    {
        tmpDir = new File("target/plugin-temp");
        if (tmpDir.exists())  FileUtils.cleanDirectory(tmpDir);
        tmpDir.mkdirs();
        cacheDir = new File(tmpDir, "felix-cache");
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

    protected void initPluginManager() throws Exception {
        initPluginManager(new HostComponentProvider(){
                public void provide(ComponentRegistrar registrar) {}
            }, new DefaultModuleDescriptorFactory());
    }

    protected void initPluginManager(HostComponentProvider hostComponentProvider) throws Exception {
        initPluginManager(hostComponentProvider, new DefaultModuleDescriptorFactory());
    }
    
    protected void initPluginManager(HostComponentProvider hostComponentProvider, ModuleDescriptorFactory moduleDescriptorFactory) throws Exception {
        initPluginManager(hostComponentProvider, moduleDescriptorFactory, null);
    }

    protected void initPluginManager(HostComponentProvider hostComponentProvider, ModuleDescriptorFactory moduleDescriptorFactory, String version) throws Exception {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(version);
        scannerConfig.getPackageIncludes().add("com.atlassian.plugin*");
        pluginEventManager = new DefaultPluginEventManager();
        osgiContainerManager = new FelixOsgiContainerManager(frameworkBundlesDir,
                                                             scannerConfig, hostComponentProvider, pluginEventManager, cacheDir);

        LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, tmpDir);
        OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, osgiContainerManager);

        DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer), new DefaultPluginEventManager());

        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader>asList(loader),
                moduleDescriptorFactory, pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }
}
