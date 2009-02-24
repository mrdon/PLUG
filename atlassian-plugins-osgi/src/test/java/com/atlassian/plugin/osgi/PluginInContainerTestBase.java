package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.repositories.FilePluginInstaller;

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
    protected File pluginsDir;
    protected ModuleDescriptorFactory moduleDescriptorFactory;
    protected DefaultPluginManager pluginManager;
    protected PluginEventManager pluginEventManager;

    @Override
    public void setUp() throws Exception
    {
        tmpDir = new File("target/plugin-temp").getAbsoluteFile();
        if (tmpDir.exists())
        {
            FileUtils.cleanDirectory(tmpDir);
        }
        tmpDir.mkdirs();
        cacheDir = new File(tmpDir, "cache");
        cacheDir.mkdir();
        pluginsDir = new File(tmpDir, "plugins");
        pluginsDir.mkdir();
        this.pluginEventManager = new DefaultPluginEventManager();
    }

    @Override
    public void tearDown() throws Exception
    {
        if (osgiContainerManager != null)
        {
            osgiContainerManager.stop();
        }
        FileUtils.deleteDirectory(tmpDir);
        osgiContainerManager = null;
        tmpDir = null;
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
            {
                registrar.register(PluginEventManager.class).forInstance(pluginEventManager);
            }
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
        scannerConfig.getPackageIncludes().add("javax.servlet*");
        scannerConfig.getPackageIncludes().add("com_cenqua_clover");
        scannerConfig.getPackageExcludes().add("com.atlassian.plugin.osgi.bridge*");
        HostComponentProvider requiredWrappingProvider = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(PluginEventManager.class).forInstance(pluginEventManager);
                if (hostComponentProvider != null)
                {
                    hostComponentProvider.provide(registrar);
                }
            }
        };
        OsgiPersistentCache cache = new DefaultOsgiPersistentCache(cacheDir, "1.0");
        osgiContainerManager = new FelixOsgiContainerManager(cache, scannerConfig, requiredWrappingProvider, pluginEventManager);

        final LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME, tmpDir);
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, cache, osgiContainerManager, pluginEventManager);
        final OsgiBundleFactory osgiBundleFactory = new OsgiBundleFactory(osgiContainerManager, pluginEventManager);

        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer, osgiBundleFactory),
            new DefaultPluginEventManager());

        pluginManager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Arrays.<PluginLoader> asList(loader), moduleDescriptorFactory,
            pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }
}
