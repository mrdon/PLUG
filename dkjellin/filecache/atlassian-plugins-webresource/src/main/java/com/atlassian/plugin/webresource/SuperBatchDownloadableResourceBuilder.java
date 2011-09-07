package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.URL_PREFIX;
import static com.google.common.collect.Iterables.concat;

/**
 * Produces a batch containing all the defined super batch dependencies
 *
 * @since 2.9.0
 */
class SuperBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private final ResourceDependencyResolver dependencyResolver;

    private final FileCache fileCache;

    public SuperBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder, FileCache fileCache)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
        this.fileCache = fileCache;
    }

    public boolean matches(final String path)
    {
        final String type = ResourceUtils.getType(path);
        return (path.indexOf(PATH_SEPARATOR + SERVLET_PATH) != -1 && path.indexOf(URL_PREFIX) != -1) && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);
    }

    public SuperBatchPluginResource parse(final String path, final Map<String, String> params)
    {
        final String type = ResourceUtils.getType(path);
        Iterable<DownloadableResource> resources = ImmutableList.of();
        for (final WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getSuperBatchDependencies())
        {
            resources = concat(resources, resolve(moduleDescriptor, type, params));
        }
        final String cachekey = ResourceUtils.buildCacheKey(path, params);
        return new SuperBatchPluginResource(type, params, resources, fileCache, cachekey);
    }

}
