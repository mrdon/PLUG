package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                                   WebResourceIntegration webResourceIntegration, DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceIntegration, resourceFinder);
        this.dependencyResolver = dependencyResolver;
    }

    public boolean matches(String path)
    {
        String type = ResourceUtils.getType(path);
        return path.indexOf(URL_PREFIX + type) > -1 && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);
    }

    public ContextBatchPluginResource parse(String path, Map<String, String> params)
    {
        ContextBatchPluginResource batchResource = getResource(path, params);

        if (log.isDebugEnabled())
        {
            log.debug(batchResource.toString());
        }

        Set<String> alreadyIncluded = new HashSet<String>();
        for (String context : batchResource.getContexts())
        {
            for (final String moduleKey : dependencyResolver.getDependenciesInContext(context))
            {
                if (!alreadyIncluded.contains(moduleKey))
                {
                    addModuleToBatch(moduleKey, batchResource);
                    alreadyIncluded.add(moduleKey);
                }
            }
        }

        return batchResource;
    }

    private ContextBatchPluginResource getResource(String path, Map<String, String> params)
    {
        final int fullStopIndex = path.lastIndexOf(".");
        final String type = path.substring(fullStopIndex + 1);

        int secondSlashIndex = path.lastIndexOf(PATH_SEPARATOR);
        int firstSlashIndex = path.lastIndexOf(PATH_SEPARATOR, secondSlashIndex - 1);
        String key = path.substring(firstSlashIndex + 1, secondSlashIndex);
        List<String> contexts = parseContexts(key);

        return new ContextBatchPluginResource(key, contexts, type, params);
    }

    private List<String> parseContexts(String key)
    {
        return Arrays.asList(key.split(ContextBatchPluginResource.CONTEXT_SEPARATOR));
    }
}
