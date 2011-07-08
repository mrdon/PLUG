package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.URL_PREFIX;
import static com.google.common.collect.Iterables.concat;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import com.google.common.collect.ImmutableList;

import java.util.Map;

/**
 * Produces a batch containing all the defined super batch dependencies
 * @since 2.9.0
 */
class SuperBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private final ResourceDependencyResolver dependencyResolver;

    public SuperBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
    }

    public boolean matches(final String path)
    {
        final String type = ResourceUtils.getType(path);
        return (path.indexOf(URL_PREFIX) != -1) && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);
    }

    public SuperBatchPluginResource parse(final String path, final Map<String, String> params)
    {
        final String type = ResourceUtils.getType(path);
        Iterable<DownloadableResource> resources = ImmutableList.of();
        for (final String moduleKey : dependencyResolver.getSuperBatchDependencies())
        {
            resources = concat(resources, resolve(moduleKey, type, params));
        }
        return new SuperBatchPluginResource(type, params, resources);
    }
}
