package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.descriptors.servlet.ServletModuleManager;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.*;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.refimpl.servlet.SimpleServletModuleDescriptor;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.BundledPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.sal.spi.HostContextAccessor;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

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
        Map<Class, Object> container = new HashMap<Class,Object>();
        hostComponentProvider = new SimpleHostComponentProvider(container);
        pluginEventManager = new DefaultPluginEventManager();
        osgiContainerManager = new FelixOsgiContainerManager(new File(servletContext.getRealPath("/WEB-INF/framework-bundles")),
                                                             scannerConfig, hostComponentProvider, pluginEventManager);

        OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, osgiContainerManager);
        OsgiBundleFactory osgiBundleDeployer = new OsgiBundleFactory(osgiContainerManager);

        DirectoryPluginLoader directoryPluginLoader = new DirectoryPluginLoader(
                new File(servletContext.getRealPath("/WEB-INF/plugins")),
                Arrays.asList(osgiPluginDeployer, osgiBundleDeployer),
                pluginEventManager);

        BundledPluginLoader bundledPluginLoader = new BundledPluginLoader(
                getClass().getResource("/atlassian-bundled-plugins.zip"),
                new File(servletContext.getRealPath("/WEB-INF/bundled-plugins")),
                Arrays.asList(osgiPluginDeployer, osgiBundleDeployer),
                pluginEventManager);

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("servlet", SimpleServletModuleDescriptor.class);
        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader>asList(bundledPluginLoader, directoryPluginLoader),
                moduleDescriptorFactory, pluginEventManager);

        container.put(PluginController.class, pluginManager);
        container.put(PluginAccessor.class, pluginManager);
        container.put(PluginEventManager.class, pluginEventManager);
        container.put(HostContextAccessor.class, new RiHostContextAccessor(container));

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
        private Map<Class, Object> container;

        public SimpleHostComponentProvider(Map<Class, Object> container)
        {
            this.container = container;
        }

        public void provide(ComponentRegistrar componentRegistrar) {
            for (Map.Entry<Class,Object> entry : container.entrySet())
            {
                String name = entry.getKey().getSimpleName();
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                componentRegistrar.register(entry.getKey()).forInstance(entry.getValue()).withName(name);
            }
        }
    }
}
