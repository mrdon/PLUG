package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.webresource.ContextBatchPluginResource.CONTEXT_SEPARATOR;
import static com.atlassian.plugin.webresource.ResourceUtils.getType;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

import com.google.common.base.Function;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Iterable<String> contexts;

    private final Iterable<String> resources;
    private final Set<PluginResourceBatchParams> resourceParams;

    ContextBatch(final String context, final List<String> resources)
    {
        key = context;
        contexts = copyOf(Arrays.asList(context));
        this.resources = copyOf(resources);
        resourceParams = newHashSet();
    }

    ContextBatch(final String key, final Iterable<String> contexts, final Iterable<String> resources, final Iterable<PluginResourceBatchParams> resourceParams)
    {
        this.key = key;
        this.contexts = copyOf(contexts);
        this.resources = copyOf(resources);
        this.resourceParams = newHashSet(resourceParams);
    }

    /**
     * Merges two context batches into a single context batch.
     * @param b1 - the context to merge into
     * @param b2 - the context to add
     * @return a single context batch.
     */
    static ContextBatch merge(final ContextBatch b1, final ContextBatch b2)
    {
        final String key = b1.getKey() + CONTEXT_SEPARATOR + b2.getKey();
        final Iterable<String> contexts = concat(b1.getContexts(), b2.getContexts());
        final Iterable<String> resources = concat(b1.getResources(), b2.getResources());
        // Merge assumes that the merged batched doesn't overlap with the current one.
        final Iterable<PluginResourceBatchParams> params = concat(b1.getResourceParams(), b2.getResourceParams());
        return new ContextBatch(key, contexts, resources, params);
    }

    boolean isResourceIncluded(final String resource)
    {
        return contains(resources, resource);
    }

    void addResourceType(final PluginResource pluginResource)
    {
        final Map<String, String> parameters = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
        final String type = getType(pluginResource.getResourceName());
        for (final String key : PluginResourceLocator.BATCH_PARAMS)
        {
            if (pluginResource.getParams().get(key) != null)
            {
                parameters.put(key, pluginResource.getParams().get(key));
            }
        }

        resourceParams.add(new PluginResourceBatchParams(type, parameters));
    }

    Iterable<PluginResource> buildPluginResources()
    {
        return transform(resourceParams, new Function<PluginResourceBatchParams, PluginResource>()
        {
            public PluginResource apply(final PluginResourceBatchParams param)
            {
                return new ContextBatchPluginResource(key, contexts, param.getType(), param.getParameters());
            }
        });
    }

    String getKey()
    {
        return key;
    }

    Iterable<String> getContexts()
    {
        return contexts;
    }

    Iterable<String> getResources()
    {
        return resources;
    }

    Iterable<PluginResourceBatchParams> getResourceParams()
    {
        return Collections.unmodifiableSet(resourceParams);
    }
}
