package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.DefaultPluginManager;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import com.atlassian.plugin.store.MemoryPluginStateStore;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Base for in-container unit tests
 */
public abstract class PluginInContainerTestBase extends TestCase
{
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
        if (tmpDir.exists())
        {
            FileUtils.cleanDirectory(tmpDir);
        }
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
        if (osgiContainerManager != null)
        {
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

    protected void initPluginManager() throws Exception
    {
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {}
        }, new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider) throws Exception
    {
        initPluginManager(hostComponentProvider, new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider, final ModuleDescriptorFactory moduleDescriptorFactory) throws Exception
    {
        initPluginManager(hostComponentProvider, moduleDescriptorFactory, null);
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider, final ModuleDescriptorFactory moduleDescriptorFactory, final String version) throws Exception
    {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        final PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(version);
        scannerConfig.getPackageIncludes().add("com.atlassian.plugin*");
        pluginEventManager = new DefaultPluginEventManager();
        osgiContainerManager = new FelixOsgiContainerManager(frameworkBundlesDir, scannerConfig, hostComponentProvider, pluginEventManager, cacheDir);

        final LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME, tmpDir);
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, osgiContainerManager);

        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer),
            new DefaultPluginEventManager());

        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader> asList(loader), moduleDescriptorFactory,
            pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }
}
