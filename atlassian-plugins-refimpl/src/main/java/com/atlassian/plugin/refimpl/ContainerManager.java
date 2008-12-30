package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.*;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.refimpl.servlet.SimpleContextListenerModuleDescriptor;
import com.atlassian.plugin.refimpl.servlet.SimpleFilterModuleDescriptor;
import com.atlassian.plugin.refimpl.servlet.SimpleServletModuleDescriptor;
import com.atlassian.plugin.refimpl.webresource.SimpleWebResourceIntegration;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import com.atlassian.plugin.servlet.*;
import com.atlassian.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceManagerImpl;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.*;

/**
 * A simple class that behaves like Spring's ContianerManager class.
 */
public class ContainerManager
{
    private static ContainerManager instance;

    private final Map<Class, Object> publicContainer;
    private final ServletModuleManager servletModuleManager;
    private final OsgiContainerManager osgiContainerManager;
    private final DefaultPluginManager pluginManager;
    private final HostComponentProvider hostComponentProvider;

    private final DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private final List<DownloadStrategy> downloadStrategies;

    public ContainerManager(ServletContext servletContext)
    {
        instance = this;

        final PluginEventManager pluginEventManager = new DefaultPluginEventManager();

        servletModuleManager = new DefaultServletModuleManager(pluginEventManager);

        DefaultPackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        List<String> packageIncludes = new ArrayList<String>(scannerConfig.getPackageIncludes());
        packageIncludes.add("org.bouncycastle*");
        packageIncludes.add("org.dom4j");
        scannerConfig.setPackageIncludes(packageIncludes);

        publicContainer = new HashMap<Class, Object>();
        hostComponentProvider = new SimpleHostComponentProvider();
        osgiContainerManager = new FelixOsgiContainerManager(new File(servletContext.getRealPath("/WEB-INF/framework-bundles")),
                scannerConfig, hostComponentProvider, pluginEventManager);

        OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, osgiContainerManager);
        OsgiBundleFactory osgiBundleDeployer = new OsgiBundleFactory(osgiContainerManager);

        DirectoryPluginLoader directoryPluginLoader = new DirectoryPluginLoader(
                new File(servletContext.getRealPath("/WEB-INF/plugins")),
                Arrays.asList(osgiPluginDeployer, osgiBundleDeployer), pluginEventManager);

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("servlet", SimpleServletModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-filter", SimpleFilterModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-param", ServletContextParamModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-listener", SimpleContextListenerModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("web-resource", WebResourceModuleDescriptor.class);
        pluginManager = new DefaultPluginManager(new MemoryPluginStateStore(), Arrays.<PluginLoader>asList(directoryPluginLoader), moduleDescriptorFactory, pluginEventManager);

        final File pluginDir = new File(servletContext.getRealPath("/WEB-INF/plugins"));
        if (!pluginDir.exists() && !pluginDir.mkdirs())
        {
            throw new RuntimeException("Could not create directory <" + pluginDir + ">");
        }
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginDir));

        publicContainer.put(PluginController.class, pluginManager);
        publicContainer.put(PluginAccessor.class, pluginManager);
        publicContainer.put(PluginEventManager.class, pluginEventManager);
        publicContainer.put(ServletModuleManager.class, servletModuleManager);
        publicContainer.put(WebResourceManager.class, new WebResourceManagerImpl(new SimpleWebResourceIntegration(servletContext)));
        publicContainer.put(Map.class, publicContainer);

        try
        {
            pluginManager.init();
        }
        catch (PluginParseException e)
        {
            throw new RuntimeException(e);
        }

        downloadStrategies = new ArrayList<DownloadStrategy>();
        PluginResourceDownload pluginDownloadStrategy = new PluginResourceDownload();
        pluginDownloadStrategy.setPluginManager(pluginManager);
        pluginDownloadStrategy.setContentTypeResolver(new SimpleContentTypeResolver());
        pluginDownloadStrategy.setCharacterEncoding("UTF-8");
        downloadStrategies.add(pluginDownloadStrategy);
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
    private static class SimpleContentTypeResolver implements ContentTypeResolver
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
            for (Map.Entry<Class, Object> entry : publicContainer.entrySet())
            {
                final String name = StringUtils.uncapitalize(entry.getKey().getSimpleName());
                componentRegistrar.register(entry.getKey()).forInstance(entry.getValue()).withName(name);
            }
        }
    }
}
