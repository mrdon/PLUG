package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.ContextBatchPluginResource.URL_PREFIX;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newHashSet;

import com.atlassian.plugin.FileCacheService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constructs a context batch resource for download
 * @since 2.9.0
 */
class ContextBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ContextBatchDownloadableResourceBuilder.class);
    private final ResourceDependencyResolver dependencyResolver;
    private final FileCacheService fileCacheService;
    private static final Set<String> validTypes = new HashSet<String>(3); //we only ever expect to have 2 entries, and 2/0.75 (the load factor) gives 2.7 so setting the size to 3 means it is never re-hashed
    static{
        validTypes.add("js");
        validTypes.add("css");
    }


    ContextBatchDownloadableResourceBuilder(final ResourceDependencyResolver dependencyResolver, final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder, final FileCacheService temp)
    {
            super(pluginAccessor, webResourceUrlProvider, resourceFinder);
            this.dependencyResolver = dependencyResolver;
            this.fileCacheService = temp;
    }

    public boolean matches(final String path)
    {
        final String type = ResourceUtils.getType(path);
        return (path.indexOf(URL_PREFIX + type) > -1) && validTypes.contains(type);
    }

    public ContextBatchPluginResource parse(final String path, final Map<String, String> params)
    {
        final String type = ResourceUtils.getType(path);
        final String key = getKey(path);
        final List<String> contexts = getContexts(key);
        final String hash = ResourceUtils.hash(ResourceUtils.getFileName(path));

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
       return new ContextBatchPluginResource(key, contexts,hash, type, params, resources,fileCacheService);
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
