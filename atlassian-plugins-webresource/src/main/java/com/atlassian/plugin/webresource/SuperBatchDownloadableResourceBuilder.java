package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import com.google.common.collect.ImmutableList;


import java.util.Map;

import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
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
    private final String tempPath;
    

    public SuperBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder)
    {
        this(dependencyResolver,pluginAccessor,webResourceUrlProvider,resourceFinder,System.getProperty("java.io.tmpdir"));
    }

    public SuperBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder, String temp)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
        this.tempPath = temp;
    }

    public boolean matches(final String path)
    {
        final String type = ResourceUtils.getType(path);

        return (path.indexOf(URL_PREFIX) != -1) && path.contains(SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX) && endsWith(path, ".", type);
    }

    public SuperBatchPluginResource parse(final String path, final Map<String, String> params)
    {
                final String type = ResourceUtils.getType(path);
                Iterable<DownloadableResource> resources = ImmutableList.of();
                for (final WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getSuperBatchDependencies())
                {
                    resources = concat(resources, resolve(moduleDescriptor, type, params));
                }
                String hash = path.substring(path.lastIndexOf("/")+1);
                int index = hash.lastIndexOf(".");
                hash = hash.substring(0,index==-1?hash.length():index);
                index = hash.indexOf("_");
                hash = hash.substring(index==-1?hash.length():index + 1);
                return new SuperBatchPluginResource(type, params, resources, hash,tempPath);
    }
}
