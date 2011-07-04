package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.atlassian.plugin.webresource.ContextBatchPluginResource.URL_PREFIX;

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
        return path.indexOf(URL_PREFIX) > -1;
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
        final int slashIndex = path.lastIndexOf("/");
        String type = path.substring(fullStopIndex + 1);
        String resourceName = path.substring(slashIndex + 1, fullStopIndex);
        return new ContextBatchPluginResource(resourceName, type, params);
    }
}
