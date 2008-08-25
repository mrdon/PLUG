package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.DefaultPluginManager;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.servlet.ServletModuleManager;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.loaders.BundledPluginLoader;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.refimpl.servlet.SimpleServletModuleDescriptor;
import com.atlassian.plugin.refimpl.webresource.SimpleWebResourceIntegration;
import com.atlassian.plugin.servlet.ContentTypeResolver;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.servlet.PluginResourceDownload;
import com.atlassian.plugin.servlet.PluginResourcesDownload;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceManagerImpl;
import com.atlassian.sal.spi.HostContextAccessor;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple class that behaves like Spring's ContianerManager class.
 */
public class ContainerManager
{
    private final ServletModuleManager servletModuleManager;
    private final WebResourceManager webResourceManager;
    private final OsgiContainerManager osgiContainerManager;
    private final DefaultPluginManager pluginManager;
    private final PluginEventManager pluginEventManager;
    private final HostComponentProvider hostComponentProvider;
    private final DefaultModuleDescriptorFactory moduleDescriptorFactory;

    private static ContainerManager instance;
    private List<DownloadStrategy> downloadStrategies;

    public ContainerManager(ServletContext servletContext)
    {
        instance = this;
        servletModuleManager = new ServletModuleManager();
        webResourceManager = new WebResourceManagerImpl(new SimpleWebResourceIntegration(servletContext));

        PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        hostComponentProvider = new SimpleHostComponentProvider();
        pluginEventManager = new DefaultPluginEventManager();
        osgiContainerManager = new FelixOsgiContainerManager(new File(servletContext.getRealPath(
            "/WEB-INF/framework-bundles")),
            scannerConfig, hostComponentProvider, pluginEventManager);

        OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME,
            osgiContainerManager);
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
        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader>asList(
            bundledPluginLoader, directoryPluginLoader),
            moduleDescriptorFactory, pluginEventManager);

        try
        {
            pluginManager.init();
        }
        catch (PluginParseException e)
        {
            e.printStackTrace();
        }

        // configure some plugin resource download strategies 
        SimpleContentTypeResolver contentTypeResolver = new SimpleContentTypeResolver();
        downloadStrategies = new ArrayList();
        downloadStrategies.add(new PluginResourcesDownload("css", "UTF-8", pluginManager, contentTypeResolver));
        downloadStrategies.add(new PluginResourcesDownload("js", "UTF-8", pluginManager, contentTypeResolver));
        downloadStrategies.add(new PluginResourceDownload("UTF-8", pluginManager, contentTypeResolver));
    }

    public static synchronized void setInstance(ContainerManager mgr)
    {
        instance = mgr;
    }

    public static synchronized ContainerManager getInstance()
    {
        return instance;
    }

    public ServletModuleManager getServletModuleManager()
    {
        return servletModuleManager;
    }

    public OsgiContainerManager getOsgiContainerManager()
    {
        return osgiContainerManager;
    }

    public PluginManager getPluginManager()
    {
        return pluginManager;
    }

    public HostComponentProvider getHostComponentProvider()
    {
        return hostComponentProvider;
    }

    public ModuleDescriptorFactory getModuleDescriptorFactory()
    {
        return moduleDescriptorFactory;
    }

    public List<DownloadStrategy> getDownloadStrategies()
    {
        return downloadStrategies;
    }

    /**
     * A simple content type resolver that can identify css and js resources.
     */
    private class SimpleContentTypeResolver implements ContentTypeResolver
    {
        private final Map<String, String> mimeTypes;

        SimpleContentTypeResolver()
        {
            Map<String, String> types = new HashMap<String, String>();
            types.put("js", "application/x-javascript");
            types.put("css", "text/css");
            mimeTypes = Collections.unmodifiableMap(types);
        }

        public String getContentType(String requestUrl)
        {
            String extension = requestUrl.substring(requestUrl.lastIndexOf('.'));
            return mimeTypes.get(extension);
        }
    }

    private class SimpleHostComponentProvider implements HostComponentProvider
    {
        public void provide(ComponentRegistrar componentRegistrar)
        {
            componentRegistrar.register(PluginManager.class, PluginAccessor.class, PluginController.class).forInstance(pluginManager).withName("pluginManager");
            componentRegistrar.register(PluginEventManager.class).forInstance(pluginEventManager).withName("pluginEventManager");
            componentRegistrar.register(HostContextAccessor.class).forInstance(new RiHostContextAccessor()).withName("hostContextAccessor");
            componentRegistrar.register(WebResourceManager.class).forInstance(webResourceManager).withName("webResourceManager");
        }
    }
}
