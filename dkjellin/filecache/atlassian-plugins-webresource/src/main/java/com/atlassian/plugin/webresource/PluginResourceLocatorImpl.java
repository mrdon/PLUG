package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.cache.filecache.impl.LRUFileCache;
import com.atlassian.plugin.cache.filecache.impl.NonCachingFileCache;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.ServletContextFactory;
import com.atlassian.plugin.util.PluginUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.atlassian.plugin.webresource.AbstractBatchResourceBuilder.skipBatch;

/**
 * Default implementation of {@link PluginResourceLocator}.
 *
 * @since 2.2
 */
public class PluginResourceLocatorImpl implements PluginResourceLocator
{
    private static final Logger log = LoggerFactory.getLogger(PluginResourceLocatorImpl.class);

    final private PluginAccessor pluginAccessor;
    final private WebResourceUrlProvider webResourceUrlProvider;
    final private ResourceBatchingConfiguration batchingConfiguration;
    final private List<DownloadableResourceBuilder> builders;

    static final String RESOURCE_SOURCE_PARAM = "source";
    static final String RESOURCE_BATCH_PARAM = "batch";

    public PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory, final WebResourceUrlProvider webResourceUrlProvider)
    {
        this(webResourceIntegration, servletContextFactory, webResourceUrlProvider, new DefaultResourceBatchingConfiguration());
    }

    public PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory, final WebResourceUrlProvider webResourceUrlProvider,
                                     final ResourceBatchingConfiguration batchingConfiguration)
    {
        this(webResourceIntegration, servletContextFactory, webResourceUrlProvider, new DefaultResourceDependencyResolver(webResourceIntegration, batchingConfiguration), batchingConfiguration);
    }

    private PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory, final WebResourceUrlProvider webResourceUrlProvider,
                                      final ResourceDependencyResolver dependencyResolver, final ResourceBatchingConfiguration batchingConfiguration)
    {

        FileCache fileCache = new NonCachingFileCache();
        if(!Boolean.getBoolean(PluginUtils.DISABLE_FILE_CACHE))
        {
            try
            {
                fileCache = new LRUFileCache(webResourceIntegration.getTemporaryDirectory(), Integer.getInteger(PluginUtils.FILE_CACHE_SIZE, 200));
            }
            catch (IOException e)
            {
                log.error("Could not create file cache object, will startup with filecaching disabled, please investigate the cause and correct it.", e);
            }
        }
        this.pluginAccessor = webResourceIntegration.getPluginAccessor();
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.batchingConfiguration = batchingConfiguration;
        final SingleDownloadableResourceBuilder singlePluginBuilder = new SingleDownloadableResourceBuilder(pluginAccessor, servletContextFactory);
        builders = Collections.unmodifiableList(Arrays.asList(new SuperBatchDownloadableResourceBuilder(dependencyResolver, pluginAccessor,
                webResourceUrlProvider, singlePluginBuilder, fileCache), new SuperBatchSubResourceBuilder(dependencyResolver, singlePluginBuilder),
                new ContextBatchDownloadableResourceBuilder(dependencyResolver, pluginAccessor, webResourceUrlProvider, singlePluginBuilder,fileCache),
                new ContextBatchSubResourceBuilder(dependencyResolver, singlePluginBuilder), new SingleBatchDownloadableResourceBuilder(pluginAccessor,
                webResourceUrlProvider, singlePluginBuilder), singlePluginBuilder));

    }

    public boolean matches(final String url)
    {
        for (final DownloadableResourceBuilder builder : builders)
        {
            if (builder.matches(url))
            {
                return true;
            }
        }

        return false;
    }

    public DownloadableResource getDownloadableResource(final String url, final Map<String, String> queryParams)
    {
        try
        {
            for (final DownloadableResourceBuilder builder : builders)
            {
                if (builder.matches(url))
                {
                    return builder.parse(url, queryParams);
                }
            }
        }
        catch (final UrlParseException e)
        {
            log.error("Unable to parse URL: " + url, e);
        }
        // TODO: It would be better to use Exceptions rather than returning
        // nulls to indicate an error.
        return null;
    }

    public List<PluginResource> getPluginResources(final String moduleCompleteKey)
    {
        final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
        if ((moduleDescriptor == null) || !(moduleDescriptor instanceof WebResourceModuleDescriptor))
        {
            log.error("Error loading resource \"" + moduleCompleteKey + "\". Resource is not a Web Resource Module");
            return Collections.emptyList();
        }

        final boolean singleMode = !batchingConfiguration.isPluginWebResourceBatchingEnabled();
        final List<PluginResource> resources = new ArrayList<PluginResource>();

        for (final ResourceDescriptor resourceDescriptor : moduleDescriptor.getResourceDescriptors())
        {
            if (singleMode || skipBatch(resourceDescriptor))
            {
                final boolean cache = !"false".equalsIgnoreCase(resourceDescriptor.getParameter("cache"));
                resources.add(new SinglePluginResource(resourceDescriptor.getName(), moduleDescriptor.getCompleteKey(), cache,
                        resourceDescriptor.getParameters()));
            }
            else
            {
                final BatchPluginResource batchResource = createBatchResource(moduleDescriptor.getCompleteKey(), resourceDescriptor);
                if (!resources.contains(batchResource))
                {
                    resources.add(batchResource);
                }
            }
        }
        return resources;
    }

    // package protected so we can test it
    String[] splitLastPathPart(final String resourcePath)
    {
        int indexOfSlash = resourcePath.lastIndexOf('/');
        if (resourcePath.endsWith("/")) // skip over the trailing slash
        {
            indexOfSlash = resourcePath.lastIndexOf('/', indexOfSlash - 1);
        }

        if (indexOfSlash < 0)
        {
            return null;
        }

        return new String[]{resourcePath.substring(0, indexOfSlash + 1), resourcePath.substring(indexOfSlash + 1)};
    }

    private BatchPluginResource createBatchResource(final String moduleCompleteKey, final ResourceDescriptor resourceDescriptor)
    {
        final String name = resourceDescriptor.getName();
        final String type = name.substring(name.lastIndexOf(".") + 1);
        final Map<String, String> params = new TreeMap<String, String>();
        for (final String param : BATCH_PARAMS)
        {
            final String value = resourceDescriptor.getParameter(param);
            if (StringUtils.isNotEmpty(value))
            {
                params.put(param, value);
            }
        }

        return new BatchPluginResource(moduleCompleteKey, type, params, Collections.<DownloadableResource>emptyList());
    }

    public String getResourceUrl(final String moduleCompleteKey, final String resourceName)
    {
        return webResourceUrlProvider.getResourceUrl(moduleCompleteKey, resourceName);
    }
}
