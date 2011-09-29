package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.webresource.ContextBatchPluginResource.URL_PREFIX;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Constructs a context batch resource for download
 *
 * @since 2.9.0
 */
class ContextBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ContextBatchDownloadableResourceBuilder.class);
    private final ResourceDependencyResolver dependencyResolver;
    private final FileCache fileCache;
    private static final Set<String> validTypes = ImmutableSet.of("js", "css");

    ContextBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder, final FileCache fileCache)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
        this.fileCache = fileCache;
    }

    public boolean matches(final String path)
    {
        final String type = ResourceUtils.getType(path);
        return (path.contains(URL_PREFIX + type)) && validTypes.contains(type);
    }

    public ContextBatchPluginResource parse(final String path, final Map<String, String> params)
    {
        final String type = ResourceUtils.getType(path);
        final String key = getKey(path);
        final List<String> contexts = getContexts(key);

        final Set<String> alreadyIncluded = newHashSet();
        Iterable<DownloadableResource> resources = ImmutableList.of();
        for (final String context : contexts)
        {
            for (final WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getDependenciesInContext(context))
            {
                String moduleKey = moduleDescriptor.getCompleteKey();
                if (!alreadyIncluded.contains(moduleKey))
                {
                    resources = concat(resources, resolve(moduleDescriptor, type, params));
                    alreadyIncluded.add(moduleKey);
                }
            }
        }
        return new ContextBatchPluginResource(key, contexts, type, params, resources, fileCache, ResourceUtils.buildCacheKey(path, params));
    }

    private String getKey(final String path)
    {
        final int secondSlashIndex = path.lastIndexOf(PATH_SEPARATOR);
        final int firstSlashIndex = path.lastIndexOf(PATH_SEPARATOR, secondSlashIndex - 1);
        return path.substring(firstSlashIndex + 1, secondSlashIndex);
    }

    private List<String> getContexts(final String key)
    {
        return Arrays.asList(key.split(ContextBatchPluginResource.CONTEXT_SEPARATOR));
    }
}
