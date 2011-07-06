package com.atlassian.plugin.webresource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.plugin.webresource.ContextBatchPluginResource.CONTEXT_SEPARATOR;

/**
 * An intermediary object used for constructing and merging context batches.
 * This is a bean that holds the different resources and parameters that apply
 * to a particular batch.
 * The batch can include one or more contexts.
 * Resources are expected to be dependency order, with no duplicates.
 */
class ContextBatch
{
    private final String key;
    private final List<String> contexts;

    private final List<String> resources;
    private final Set<PluginResourceBatchParams> resourceParams;

    ContextBatch(String context, List<String> resources)
    {
        this.key = context;
        contexts = ImmutableList.copyOf(Arrays.asList(context));

        this.resources = ImmutableList.copyOf(resources);
        this.resourceParams = new HashSet<PluginResourceBatchParams>();
    }

    ContextBatch(String key, List<String> contexts, List<String> resources, Set<PluginResourceBatchParams> resourceParams)
    {
        this.key = key;
        this.contexts = ImmutableList.copyOf(contexts);

        this.resources = ImmutableList.copyOf(resources);
        this.resourceParams = ImmutableSet.copyOf(resourceParams);
    }

    /**
     * Merges two context batches into a single context batch.
     * @param contextBatch1 - the context to merge into
     * @param contextBatch2 - the context to add
     * @return a single context batch.
     */
    public static ContextBatch merge(ContextBatch contextBatch1, ContextBatch contextBatch2)
    {
        String newKey = contextBatch1.getKey() + CONTEXT_SEPARATOR + contextBatch2.getKey();

        List<String> newContexts = new ArrayList<String>(contextBatch1.getContexts());
        newContexts.addAll(contextBatch2.getContexts());

        List<String> newResources = new ArrayList<String>(contextBatch1.getResources());
        newResources.addAll(contextBatch2.getResources());

        // Merge assumes that the merged batched doesn't overlap with the current one.
        Set<PluginResourceBatchParams> newResourceParams = new HashSet<PluginResourceBatchParams>(contextBatch1.getResourceParams());
        newResourceParams.addAll(contextBatch2.getResourceParams());

        return new ContextBatch(newKey, newContexts, newResources, newResourceParams);
    }

    public boolean isResourceIncluded(String resource)
    {
        return resources.contains(resource);
    }

    public void addResourceType(PluginResource pluginResource)
    {
        Map<String, String> parameters = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
        String type = ResourceUtils.getType(pluginResource.getResourceName());
        for (String s : PluginResourceLocator.BATCH_PARAMS)
        {
            if (pluginResource.getParams().get(s) != null)
            {
                parameters.put(s, pluginResource.getParams().get(s));
            }
        }

        PluginResourceBatchParams resourceType = new PluginResourceBatchParams(type, parameters);

        if (!resourceParams.contains(resourceType))
        {
            resourceParams.add(resourceType);
        }
    }

    public List<ContextBatchPluginResource> buildPluginResources()
    {
        List<ContextBatchPluginResource> pluginResources = new ArrayList<ContextBatchPluginResource>();
        for (PluginResourceBatchParams resourceParam : resourceParams)
        {
            pluginResources.add(new ContextBatchPluginResource(key, contexts, resourceParam.getType(), resourceParam.getParameters()));
        }

        return  pluginResources;
    }

    public String getKey()
    {
        return key;
    }

    public List<String> getContexts()
    {
        return contexts;
    }

    public List<String> getResources()
    {
        return resources;
    }

    public Set<PluginResourceBatchParams> getResourceParams()
    {
        return Collections.unmodifiableSet(resourceParams);
    }
}
