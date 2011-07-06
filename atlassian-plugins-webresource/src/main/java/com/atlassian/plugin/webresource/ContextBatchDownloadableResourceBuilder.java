package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.ContextBatchPluginResource.URL_PREFIX;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX;

public class ContextBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ContextBatchDownloadableResourceBuilder.class);
    private final ResourceDependencyResolver dependencyResolver;

    public ContextBatchDownloadableResourceBuilder(ResourceDependencyResolver dependencyResolver, PluginAccessor pluginAccessor,
                                                   WebResourceUrlProvider webResourceUrlProvider, DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
    }

    public boolean matches(String path)
    {
        String type = ResourceUtils.getType(path);
        return path.indexOf(URL_PREFIX + type) > -1 && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);
    }

    public ContextBatchPluginResource parse(String path, Map<String, String> params)
    {
        String type = ResourceUtils.getType(path);
        String key = getKey(path);
        List<String> contexts = getContexts(key);

        Set<String> alreadyIncluded = new HashSet<String>();
        List<DownloadableResource> resources = new ArrayList<DownloadableResource>();
        for (String context : contexts)
        {
            for (final String moduleKey : dependencyResolver.getDependenciesInContext(context))
            {
                if (!alreadyIncluded.contains(moduleKey))
                {
                    resources.addAll(resolve(moduleKey, type, params));
                    alreadyIncluded.add(moduleKey);
                }
            }
        }

        ContextBatchPluginResource batchResource = new ContextBatchPluginResource(key, contexts, type, params, resources);

        if (log.isDebugEnabled())
        {
            log.debug(batchResource.toString());
        }

        return batchResource;
    }

    private String getKey(String path)
    {
        int secondSlashIndex = path.lastIndexOf(PATH_SEPARATOR);
        int firstSlashIndex = path.lastIndexOf(PATH_SEPARATOR, secondSlashIndex - 1);
        return path.substring(firstSlashIndex + 1, secondSlashIndex);
    }

    private List<String> getContexts(String key)
    {
        return Arrays.asList(key.split(ContextBatchPluginResource.CONTEXT_SEPARATOR));
    }
}
