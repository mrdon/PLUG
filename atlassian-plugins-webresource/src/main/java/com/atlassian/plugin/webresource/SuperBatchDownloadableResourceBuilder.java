package com.atlassian.plugin.webresource;

import com.atlassian.plugin.FileCacheService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final FileCacheService fileCacheService;
    private static final Set<String> validTypes = new HashSet<String>(3); //we only ever expect to have 2 entries, and 2/0.75 (the load factor) gives 2.7 so setting the size to 3 means it is never re-hashed
    static{
        validTypes.add("js");
        validTypes.add("css");
    }


    public SuperBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder, FileCacheService fileCacheService)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
        this.fileCacheService = fileCacheService;
    }

    public boolean matches(final String path)
    {
        return (path.indexOf(URL_PREFIX) != -1) && validTypes.contains(ResourceUtils.getType(path));
    }

    public SuperBatchPluginResource parse(final String path, final Map<String, String> params)
    {
                final String type = ResourceUtils.getType(path);
                Iterable<DownloadableResource> resources = ImmutableList.of();
                for (final WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getSuperBatchDependencies())
                {
                    resources = concat(resources, resolve(moduleDescriptor, type, params));
                }
                String toHash= ResourceUtils.getFileName(path);
                if(!params.isEmpty())
                {
                    StringBuilder sb = new StringBuilder(toHash);
                    for(Map.Entry<String,String> entry:params.entrySet())
                    {
                        sb.append(entry.getKey()).append(entry.getValue());
                    }
                    toHash = sb.toString();
                }
                String hash = ResourceUtils.hash(toHash);

                return new SuperBatchPluginResource(type, params, resources, hash, fileCacheService);
    }
}
