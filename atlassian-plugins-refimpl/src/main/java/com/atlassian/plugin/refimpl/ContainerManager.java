package com.atlassian.plugin.refimpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

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
import com.atlassian.plugin.refimpl.servlet.SimpleServletContextFactory;
import com.atlassian.plugin.refimpl.webresource.SimpleWebResourceIntegration;
import com.atlassian.plugin.servlet.ContentTypeResolver;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.servlet.PluginResourceDownload;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.util.Assertions;
import com.atlassian.plugin.webresource.PluginResourceLocator;
import com.atlassian.plugin.webresource.PluginResourceLocatorImpl;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.WebResourceManagerImpl;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;

/**
 * A simple class that behaves like Spring's ContainerManager class.
 */
public class ContainerManager
{
    /**
     * the name of the bundled plugins zip file to use
     */
    private static final String BUNDLED_PLUGINS_ZIP = "/atlassian-bundled-plugins.zip";

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

        // Delegating host container since the real one requires the created object map, which won't be available until later
        final HostContainer delegatingHostContainer = new HostContainer()
        {
            public <T> T create(final Class<T> moduleClass) throws IllegalArgumentException
            {
                return hostContainer.create(moduleClass);
            }
        };

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(delegatingHostContainer);

        moduleDescriptorFactory.addModuleDescriptor("servlet", ServletModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-filter", ServletFilterModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-param", ServletContextParamModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-listener", ServletContextListenerModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("web-resource", WebResourceModuleDescriptor.class);

        final DefaultPackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(determineVersion());
        final List<String> packageIncludes = new ArrayList<String>(scannerConfig.getPackageIncludes());
        packageIncludes.add("org.bouncycastle*");
        packageIncludes.add("org.dom4j*");
        packageIncludes.add("javax.servlet*");
        packageIncludes.add("com.opensymphony.module.sitemesh*");

        scannerConfig.setPackageIncludes(packageIncludes);
        scannerConfig.setPackageVersions(new HashMap<String,String>() {{
            put("javax.servlet", "2.5");
            put("javax.servlet.http", "2.5");
        }});
        hostComponentProvider = new SimpleHostComponentProvider();

        File osgiCache;
        if (System.getProperty("osgi.cache") != null)
        {
            osgiCache = makeSureDirectoryExists(System.getProperty("osgi.cache"));
        }
        else
        {
            osgiCache = makeSureDirectoryExists(servletContext, "/WEB-INF/osgi-cache");
        }

        final PluginsConfiguration config = new PluginsConfigurationBuilder()
                .useLegacyDynamicPluginDeployer(true)
                .bundledPluginUrl(this.getClass().getResource(BUNDLED_PLUGINS_ZIP))
                .bundledPluginCacheDirectory(makeSureDirectoryExists(servletContext, "/WEB-INF/bundled-plugins"))
                .pluginDirectory(makeSureDirectoryExists(servletContext, "/WEB-INF/plugins"))
                .moduleDescriptorFactory(moduleDescriptorFactory)
                .packageScannerConfiguration(scannerConfig)
                .hostComponentProvider(hostComponentProvider)
                .osgiPersistentCache(osgiCache)
                .pluginStateStore(new DefaultPluginPersistentStateStore(osgiCache))
                .applicationKey("refapp")
                .build();
        plugins = new AtlassianPlugins(config);

        final PluginEventManager pluginEventManager = plugins.getPluginEventManager();
        osgiContainerManager = plugins.getOsgiContainerManager();

        servletModuleManager = new DefaultServletModuleManager(servletContext, pluginEventManager);
        pluginAccessor = plugins.getPluginAccessor();

        final PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl(pluginAccessor, new SimpleServletContextFactory(servletContext));
        final PluginResourceDownload pluginDownloadStrategy = new PluginResourceDownload(pluginResourceLocator, new SimpleContentTypeResolver(), "UTF-8");

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, webResourceIntegration);

        publicContainer = new HashMap<Class<?>, Object>();
        publicContainer.put(PluginController.class, plugins.getPluginController());
        publicContainer.put(PluginAccessor.class, pluginAccessor);
        publicContainer.put(ServletModuleManager.class, servletModuleManager);
        publicContainer.put(WebResourceManager.class, webResourceManager);
        publicContainer.put(Map.class, publicContainer);

        hostContainer = new SimpleConstructorHostContainer(publicContainer);

        try
        {
            plugins.start();
        }
        catch (final PluginParseException e)
        {
            throw new RuntimeException(e);
        }

        downloadStrategies = new ArrayList<DownloadStrategy>();
        downloadStrategies.add(pluginDownloadStrategy);
    }

    private String determineVersion()
    {
        InputStream in = null;
        final Properties props = new Properties();
        try
        {
            in = getClass().getClassLoader().getResourceAsStream("META-INF/maven/com.atlassian.plugins/atlassian-plugins-core/pom.properties");
            if (in != null)
            {
                props.load(in);
                return props.getProperty("version");
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    private File makeSureDirectoryExists(final ServletContext servletContext, final String relativePath)
    {
        return makeSureDirectoryExists(servletContext.getRealPath(relativePath));
    }

    private File makeSureDirectoryExists(final String path)
    {
        final File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new RuntimeException("Could not create directory <" + dir + ">");
        }
        return dir;
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
            Assertions.notNull("publicContainer", publicContainer);
            for (final Map.Entry<Class<?>, Object> entry : publicContainer.entrySet())
            {
                final String name = StringUtils.uncapitalize(entry.getKey().getSimpleName());
                componentRegistrar.register(entry.getKey()).forInstance(entry.getValue()).withName(name);
            }

        }
    }
}
