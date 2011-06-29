package com.atlassian.plugin.webresource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A intermediary object used for constructing and merging context batches.
 */
public class ContextBatch
{
    private String key;
    private List<String> contexts;

    private List<String> resources;
    private Set<ResourceType> resourceTypes;

    public ContextBatch(String context, List<String> resources)
    {
        this.key = context;
        contexts = new ArrayList<String>();
        contexts.add(context);

        this.resources = resources;
        this.resourceTypes = new HashSet<ResourceType>();
    }

    public void merge(ContextBatch contextBatch)
    {
        this.key += "+" + contextBatch.getKey();
        this.contexts.addAll(contextBatch.getContexts());

        // Merge assumes that the merged batched doesn't overlap with the current one.
        this.resources.addAll(contextBatch.getResources());
        this.resourceTypes.addAll(contextBatch.getResourceTypes());
    }

    public boolean isResourceIncluded(String resource)
    {
        return resources.contains(resource);
    }

    public void addResourceType(PluginResource pluginResource)
    {
        Map<String, String> parameters = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
        String type = getType(pluginResource.getResourceName());
        for (String s : PluginResourceLocator.BATCH_PARAMS)
        {
            if (pluginResource.getParams().get(s) != null)
                parameters.put(s, pluginResource.getParams().get(s));
        }

        ResourceType resourceType = new ResourceType(type, parameters);

        if (!resourceTypes.contains(resourceType))
        {
            resourceTypes.add(resourceType);
        }
    }

    public List<ContextBatchPluginResource> buildPluginResources()
    {
        List<ContextBatchPluginResource> pluginResources = new ArrayList<ContextBatchPluginResource>();
        for (ResourceType resourceType : resourceTypes)
        {
            pluginResources.add(new ContextBatchPluginResource(key, resourceType.getType(), resourceType.getParameters()));
        }

        return  pluginResources;
    }

    private static String getType(String path)
    {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
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

    public Set<ResourceType> getResourceTypes()
    {
        return resourceTypes;
    }
}
