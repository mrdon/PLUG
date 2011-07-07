package com.atlassian.plugin.webresource;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs a calculation on many referenced contexts, and produces an set of intermingled batched-contexts and residual
 * (skipped) resources. Some of the input contexts may have been merged into cross-context batches.
 * The batches are constructed in such a way that no batch is dependent on another.
 * The output batches and resources may be intermingled so as to preserve the input order as much as possible.
 *
 * @since 2.10
 */
class ContextBatchBuilder
{
    private static final Logger log = LoggerFactory.getLogger(WebResourceManagerImpl.class);

    private final PluginResourceLocator pluginResourceLocator;
    private final ResourceDependencyResolver dependencyResolver;

    private final List<String> allIncludedResources = new ArrayList<String>();
    private final Set<String> skippedResources = new HashSet<String>();

    ContextBatchBuilder(final PluginResourceLocator pluginResourceLocator, final ResourceDependencyResolver dependencyResolver)
    {
        this.pluginResourceLocator = pluginResourceLocator;
        this.dependencyResolver = dependencyResolver;
    }

    Iterable<PluginResource> build(final Iterable<String> includedContexts)
    {
        return build(includedContexts, DefaultWebResourceFilter.INSTANCE);
    }

    Iterable<PluginResource> build(final Iterable<String> includedContexts, final WebResourceFilter filter)
    {
        // There are three levels to consider here. In order:
        // 1. Type (CSS/JS)
        // 2. Parameters (ieOnly, media, etc)
        // 3. Context
        final List<ContextBatch> batches = new ArrayList<ContextBatch>();

        for (final String context : includedContexts)
        {
            final ContextBatch contextBatch = new ContextBatch(context, dependencyResolver.getDependenciesInContext(context, skippedResources));
            final List<ContextBatch> mergeList = new ArrayList<ContextBatch>();
            for (final String contextResource : contextBatch.getResources())
            {
                // only go deeper if it is not already included
                if (!allIncludedResources.contains(contextResource))
                {
                    for (final PluginResource pluginResource : pluginResourceLocator.getPluginResources(contextResource))
                    {
                        if (filter.matches(pluginResource.getResourceName()))
                        {
                            contextBatch.addResourceType(pluginResource);
                        }
                    }

                    allIncludedResources.add(contextResource);
                }
                else
                {
                    // we have an overlapping context, find it.
                    // IMPORTANT: Don't add the overlapping resource to the batch otherwise there'll be duplicates
                    for (final ContextBatch batch : batches)
                    {
                        if (!mergeList.contains(batch) && batch.isResourceIncluded(contextResource))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Context: {} shares a resource with {}: {}", new String[] { context, batch.getKey(), contextResource });
                            }

                            mergeList.add(batch);
                        }
                    }
                }
            }

            // Merge all the flagged contexts
            if (!mergeList.isEmpty())
            {
                ContextBatch mergedBatch = mergeList.get(0);
                batches.remove(mergedBatch);

                for (int i = 1; i < mergeList.size(); i++)
                {
                    final ContextBatch mergingBatch = mergeList.get(i);
                    mergedBatch = ContextBatch.merge(mergedBatch, mergingBatch);
                    batches.remove(mergingBatch);
                }

                mergedBatch = ContextBatch.merge(mergedBatch, contextBatch);
                batches.add(mergedBatch);
            }
            else
            {
                // Otherwise just add a new one
                batches.add(contextBatch);
            }
        }

        // Build the batch resources
        return concat(transform(batches, new Function<ContextBatch, Iterable<PluginResource>>()
        {
            public Iterable<PluginResource> apply(final ContextBatch batch)
            {
                return batch.buildPluginResources();
            }
        }));
    }

    Iterable<String> getAllIncludedResources()
    {
        return allIncludedResources;
    }

    Iterable<String> getSkippedResources()
    {
        return skippedResources;
    }
}
