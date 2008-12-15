package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.hostcontainer.SimpleConstructorHostContainer;
import com.atlassian.plugin.main.AtlassianPlugins;
import com.atlassian.plugin.main.PluginsConfiguration;
import com.atlassian.plugin.main.PluginsConfigurationBuilder;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.refimpl.webresource.SimpleWebResourceIntegration;
import com.atlassian.plugin.refimpl.servlet.SimpleServletContextFactory;
import com.atlassian.plugin.servlet.ContentTypeResolver;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.servlet.PluginResourceDownload;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.util.ServletContextHostContainerAccessor;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceManagerImpl;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.atlassian.plugin.webresource.PluginResourceLocatorImpl;
import com.atlassian.plugin.webresource.PluginResourceLocator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

/**
 * A simple class that behaves like Spring's ContainerManager class.
 */
public class ContainerManager
{
    private final ServletModuleManager servletModuleManager;
    private final SimpleWebResourceIntegration webResourceIntegration;
    private final WebResourceManager webResourceManager;
    private final OsgiContainerManager osgiContainerManager;
    private final PluginAccessor pluginAccessor;
    private final HostComponentProvider hostComponentProvider;
    private final DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private final Map<Class<?>, Object> publicContainer;
    private final AtlassianPlugins plugins;
    private final HostContainer hostContainer;

    private static ContainerManager instance;
    private final List<DownloadStrategy> downloadStrategies;

    public ContainerManager(final ServletContext servletContext)
    {
        instance = this;
        webResourceIntegration = new SimpleWebResourceIntegration(servletContext);

        final File pluginDir = new File(servletContext.getRealPath("/WEB-INF/plugins"));
        if (!pluginDir.exists())
        {
            pluginDir.mkdirs();
        }
        final File bundlesDir = new File(servletContext.getRealPath("/WEB-INF/framework-bundles"));
        if (!bundlesDir.exists())
        {
            bundlesDir.mkdirs();
        }

        // Delegating host container since the real one requires the created object map, which won't be available
        // until later
        final HostContainer delegatingHostContainer = new HostContainer()
        {
            public <T> T create(final Class<T> moduleClass) throws IllegalArgumentException
            {
                return hostContainer.create(moduleClass);
            }

            public <T> T getInstance(final Class<T> moduleClass)
            {
                return hostContainer.getInstance(moduleClass);
            }
        };
        ServletContextHostContainerAccessor.setHostContainer(servletContext, delegatingHostContainer);

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(delegatingHostContainer);

        moduleDescriptorFactory.addModuleDescriptor("servlet", ServletModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-filter", ServletFilterModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-param", ServletContextParamModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-listener", ServletContextListenerModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("web-resource", WebResourceModuleDescriptor.class);

        final DefaultPackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration();
        final List<String> packageIncludes = new ArrayList<String>(scannerConfig.getPackageIncludes());
        packageIncludes.add("org.bouncycastle*");
        scannerConfig.setPackageIncludes(packageIncludes);
        hostComponentProvider = new SimpleHostComponentProvider();

        final PluginsConfiguration config = new PluginsConfigurationBuilder()
            .pluginDirectory(pluginDir)
            .moduleDescriptorFactory(moduleDescriptorFactory)
            .packageScannerConfiguration(scannerConfig)
            .hostComponentProvider(hostComponentProvider)
            .frameworkBundlesDirectory(bundlesDir)
            .build();
        plugins = new AtlassianPlugins(config);

        final PluginEventManager pluginEventManager = plugins.getPluginEventManager();
        osgiContainerManager = plugins.getOsgiContainerManager();

        servletModuleManager = new DefaultServletModuleManager(pluginEventManager);

        publicContainer = new HashMap<Class<?>, Object>();

        pluginAccessor = plugins.getPluginAccessor();
        publicContainer.put(PluginController.class, plugins.getPluginController());
        publicContainer.put(PluginAccessor.class, pluginAccessor);
        publicContainer.put(PluginEventManager.class, pluginEventManager);
        publicContainer.put(ServletModuleManager.class, servletModuleManager);
        publicContainer.put(Map.class, publicContainer);

        hostContainer = new SimpleConstructorHostContainer(publicContainer);

        try
        {
            plugins.start();
        }
        catch (final PluginParseException e)
        {
            e.printStackTrace();
        }

        PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl(pluginAccessor, new SimpleServletContextFactory(servletContext));
        PluginResourceDownload pluginDownloadStrategy = new PluginResourceDownload(pluginResourceLocator, new SimpleContentTypeResolver(), "UTF-8");

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, webResourceIntegration);
        downloadStrategies = new ArrayList<DownloadStrategy>();
        downloadStrategies.add(pluginDownloadStrategy);
    }

    public static synchronized void setInstance(final ContainerManager mgr)
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

    public PluginAccessor getPluginAccessor()
    {
        return pluginAccessor;
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

    public WebResourceManager getWebResourceManager()
    {
        return webResourceManager;
    }

    public WebResourceIntegration getWebResourceIntegration()
    {
        return webResourceIntegration;
    }

    void shutdown()
    {
        plugins.stop();
    }

    /**
     * A simple content type resolver that can identify css and js resources.
     */
    private class SimpleContentTypeResolver implements ContentTypeResolver
    {
        private final Map<String, String> mimeTypes;

        SimpleContentTypeResolver()
        {
            final Map<String, String> types = new HashMap<String, String>();
            types.put("js", "application/x-javascript");
            types.put("css", "text/css");
            mimeTypes = Collections.unmodifiableMap(types);
        }

        public String getContentType(final String requestUrl)
        {
            final String extension = requestUrl.substring(requestUrl.lastIndexOf('.'));
            return mimeTypes.get(extension);
        }
    }

    private class SimpleHostComponentProvider implements HostComponentProvider
    {
        public void provide(final ComponentRegistrar componentRegistrar)
        {
            for (final Map.Entry<Class<?>, Object> entry : publicContainer.entrySet())
            {
                String name = entry.getKey().getSimpleName();
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                componentRegistrar.register(entry.getKey()).forInstance(entry.getValue()).withName(name);
            }
            componentRegistrar.register(WebResourceManager.class).forInstance(webResourceManager).withName("webResourceManager");
        }
    }
}
