package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.descriptors.servlet.ServletModuleManager;
import com.atlassian.plugin.descriptors.servlet.ServletModuleDescriptor;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.loader.OsgiPluginLoader;
import com.atlassian.plugin.osgi.loader.BundledOsgiPluginLoader;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.*;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.PluginEventManagerImpl;
import com.atlassian.plugin.refimpl.servlet.SimpleServletModuleDescriptor;
import com.atlassian.plugin.loaders.DefaultPluginFactory;
import com.atlassian.plugin.store.MemoryPluginStateStore;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 06/07/2008
 * Time: 12:29:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContainerManager {

    private final ServletModuleManager servletModuleManager;
    private final OsgiContainerManager osgiContainerManager;
    private final DefaultPluginManager pluginManager;
    private final PluginEventManager pluginEventManager;
    private final HostComponentProvider hostComponentProvider;
    private final DefaultModuleDescriptorFactory moduleDescriptorFactory;

    private static ContainerManager instance;

    public ContainerManager(ServletContext servletContext) {
        instance = this;
        servletModuleManager = new ServletModuleManager();

        PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        osgiContainerManager = new FelixOsgiContainerManager(new File(servletContext.getRealPath("/WEB-INF/framework-bundles")),
                                                             scannerConfig);

        hostComponentProvider = new SimpleHostComponentProvider();
        OsgiPluginLoader osgiPluginLoader = new OsgiPluginLoader(
                new File(servletContext.getRealPath("/WEB-INF/plugins")),
                PluginManager.PLUGIN_DESCRIPTOR_FILENAME,
                new DefaultPluginFactory(),
                osgiContainerManager,
                hostComponentProvider);

        BundledOsgiPluginLoader bundledPluginLoader = new BundledOsgiPluginLoader(
                getClass().getResource("/atlassian-bundled-plugins.zip"),
                new File(servletContext.getRealPath("/WEB-INF/bundled-plugins")),
                PluginManager.PLUGIN_DESCRIPTOR_FILENAME,
                new DefaultPluginFactory(),
                osgiContainerManager,
                hostComponentProvider);
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("servlet", SimpleServletModuleDescriptor.class);
        pluginEventManager = new PluginEventManagerImpl();
        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.asList(bundledPluginLoader, osgiPluginLoader),
                moduleDescriptorFactory, pluginEventManager);
        try {
            pluginManager.init();
        } catch (PluginParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public static synchronized void setInstance(ContainerManager mgr) {
        instance = mgr;
    }

    public static synchronized ContainerManager getInstance() {
        return instance;
    }

    public ServletModuleManager getServletModuleManager() {
        return servletModuleManager;
    }

    public OsgiContainerManager getOsgiContainerManager() {
        return osgiContainerManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public HostComponentProvider getHostComponentProvider() {
        return hostComponentProvider;
    }

    public ModuleDescriptorFactory getModuleDescriptorFactory() {
        return moduleDescriptorFactory;
    }

    private class SimpleHostComponentProvider implements HostComponentProvider {

        public void provide(ComponentRegistrar componentRegistrar) {
            componentRegistrar.register(PluginManager.class, PluginAccessor.class, PluginController.class).forInstance(pluginManager).withName("pluginManager");
            componentRegistrar.register(PluginEventManager.class).forInstance(pluginEventManager).withName("pluginEventManager");
        }
    }
}
