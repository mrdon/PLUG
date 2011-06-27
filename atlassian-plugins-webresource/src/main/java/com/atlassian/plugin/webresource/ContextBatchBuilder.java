package com.atlassian.plugin.webresource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextBatchBuilder
{
    private static final Logger log = LoggerFactory.getLogger(WebResourceManagerImpl.class);

    private final WebResourceIntegration webResourceIntegration;
    private final PluginResourceLocator pluginResourceLocator;
    private final ResourceBatchingConfiguration batchingConfiguration;
    private final ResourceDependencyResolver dependencyResolver;

    private List<String> allIncludedResources = new ArrayList<String>();
    private List<PluginResource> contextBatches = new ArrayList<PluginResource>();

    public ContextBatchBuilder(WebResourceIntegration webResourceIntegration, PluginResourceLocator pluginResourceLocator, ResourceBatchingConfiguration batchingConfiguration, ResourceDependencyResolver dependencyResolver)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.pluginResourceLocator = pluginResourceLocator;
        this.batchingConfiguration = batchingConfiguration;
        this.dependencyResolver = dependencyResolver;
    }

    public List<PluginResource> build(Set<String> includedContexts)
    {
        return build(includedContexts, DefaultWebResourceFilter.INSTANCE);
    }

    public List<PluginResource> build(Set<String> includedContexts, WebResourceFilter filter)
    {
        // There are three levels to consider here. In order:
        // 1. Type (CSS/JS)
        // 2. Parameters (ieOnly, media, etc)
        // 3. Context

        // TODO - put this into its own object
        Map<String, List<String>> resourcesPerContext = new HashMap<String, List<String>>();
        Map<String, Set<Map<String, String>>> paramsPerContext = new HashMap<String, Set<Map<String, String>>>();

        for (String context : includedContexts)
        {
            List<String> contextResources = getContextResources(context);
            Set<Map<String, String>> params = new HashSet<Map<String, String>>();
            List<String> mergeList = new ArrayList<String>();
            for (String contextResource : contextResources)
            {
                // only go deeper if it is not already included
                if (!allIncludedResources.contains(contextResource))
                {
                    for (PluginResource pluginResource : pluginResourceLocator.getPluginResources(contextResource))
                    {
                        if (filter.matches(pluginResource.getResourceName()))
                        {
                            Map<String, String> batchParamsMap = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
                            batchParamsMap.put("type", getType(pluginResource.getResourceName()));
                            for (String s : PluginResourceLocator.BATCH_PARAMS)
                            {
                                if (pluginResource.getParams().get(s) != null)
                                    batchParamsMap.put(s, pluginResource.getParams().get(s));
                            }
                            if (!params.contains(batchParamsMap))
                            {
                                params.add(batchParamsMap);
                            }
                        }
                    }

                    allIncludedResources.add(contextResource);
                }
                else
                {
                    // we have an overlapping context, find it.
                    for (String processed : resourcesPerContext.keySet())
                    {
                        if (!mergeList.contains(processed) && resourcesPerContext.get(processed).contains(contextResource))
                        {
                            log.debug("Context: {} shares a resource with {}: {}", new String[] {context, processed, contextResource});
                            mergeList.add(processed);
                        }
                    }
                }
            }

            // Merge all the flagged contexts
            // context key is now contextA+contextB
            String mergedContext = "";
            List<String> mergedContextResources = new ArrayList<String>();
            Set<Map<String, String>> mergedParams = new HashSet<Map<String, String>>();
            for (String toMerge : mergeList)
            {
                mergedContext += toMerge + "+";
                mergedContextResources.addAll(resourcesPerContext.get(toMerge));

                // TODO - only merge overlapping types?
                mergedParams.addAll(paramsPerContext.get(toMerge));

                resourcesPerContext.remove(toMerge);
                paramsPerContext.remove(toMerge);
            }

            mergedContext += context;
            mergedContextResources.addAll(contextResources);
            mergedParams.addAll(params);

            // After we've processed the context, add it to the list.
            paramsPerContext.put(mergedContext, mergedParams);
            resourcesPerContext.put(mergedContext, mergedContextResources);
        }

        // Build the batch resources
        for (String contextBatch : resourcesPerContext.keySet())
        {
            for (Map<String, String> params : paramsPerContext.get(contextBatch))
            {
                String type = params.get("type");
                params.remove("type");

                contextBatches.add(new ContextBatchPluginResource(contextBatch, type, params));
            }
        }

        return contextBatches;
    }

    public List<String> getAllIncludedResources()
    {
        return allIncludedResources;
    }

    /**
     * Returns all the resources (including dependencies) within a given context.
     * @param context a context to search
     * @return all resources within a context
     */
    private List<String> getContextResources(String context)
    {
        List<String> contextResources = new ArrayList<String>();
        List<WebResourceModuleDescriptor> descriptors = webResourceIntegration.getPluginAccessor().getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class);
        for (WebResourceModuleDescriptor descriptor : descriptors)
        {
            if (descriptor.getContexts().contains(context))
            {
                LinkedHashSet<String> dependencies = dependencyResolver.getDependencies(descriptor.getCompleteKey(), true);
                for (String dependency : dependencies) {
                    if (!contextResources.contains(dependency))
                    {
                        contextResources.add(dependency);
                    }
                }
            }
        }
        return contextResources;
    }

    private static String getType(String path)
    {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }
}
