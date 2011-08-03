package com.atlassian.plugin.webresource;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs a calculation on many referenced contexts, and produces an set of intermingled batched-contexts and residual
 * (skipped) resources. Some of the input contexts may have been merged into cross-context batches.
 * The batches are constructed in such a way that no batch is dependent on another.
 * The output batches and resources may be intermingled so as to preserve the input order as much as possible.
 *
 * @since 2.9.0
 */
class ContextBatchBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ContextBatchBuilder.class);

    private final PluginResourceLocator pluginResourceLocator;
    private final ResourceDependencyResolver dependencyResolver;
    private final ResourceBatchingConfiguration batchingConfiguration;
    private final String tempDir;

    private final List<String> allIncludedResources = new ArrayList<String>();
    private final Set<String> skippedResources = new HashSet<String>();

    ContextBatchBuilder(final PluginResourceLocator pluginResourceLocator, final ResourceDependencyResolver dependencyResolver, ResourceBatchingConfiguration batchingConfiguration)
    {
        this(pluginResourceLocator,dependencyResolver,batchingConfiguration,System.getProperty("java.io.tmpdir"));
    }

    ContextBatchBuilder(final PluginResourceLocator pluginResourceLocator, final ResourceDependencyResolver dependencyResolver, ResourceBatchingConfiguration batchingConfiguration, final String temp)
    {
        this.pluginResourceLocator = pluginResourceLocator;
        this.dependencyResolver = dependencyResolver;
        this.batchingConfiguration = batchingConfiguration;
        this.tempDir = temp;
    }

    Iterable<PluginResource> build(final Iterable<String> includedContexts)
    {
        return build(includedContexts, DefaultWebResourceFilter.INSTANCE);
    }

    Iterable<PluginResource> build(final Iterable<String> includedContexts, final WebResourceFilter filter)
    {
        if (!batchingConfiguration.isContextBatchingEnabled())
        {
             return getUnbatchedResources(includedContexts, filter);
        }

        // There are three levels to consider here. In order:
        // 1. Type (CSS/JS)
        // 2. Parameters (ieOnly, media, etc)
        // 3. Context
        final List<ContextBatch> batches = new ArrayList<ContextBatch>();

        for (final String context : includedContexts)
        {
            final ContextBatch contextBatch = new ContextBatch(context, dependencyResolver.getDependenciesInContext(context, skippedResources),tempDir);
            final List<ContextBatch> mergeList = new ArrayList<ContextBatch>();
            for (final WebResourceModuleDescriptor contextResource : contextBatch.getResources())
            {
                // only go deeper if it is not already included
                if (!allIncludedResources.contains(contextResource.getCompleteKey()))
                {
                    for (final PluginResource pluginResource : pluginResourceLocator.getPluginResources(contextResource.getCompleteKey()))
                    {
                        if (filter.matches(pluginResource.getResourceName()))
                        {
                            contextBatch.addResourceType(pluginResource);
                        }
                    }

                    allIncludedResources.add(contextResource.getCompleteKey());
                }
                else
                {
                    // we have an overlapping context, find it.
                    // IMPORTANT: Don't add the overlapping resource to the batch otherwise there'll be duplicates
                    for (final ContextBatch batch : batches)
                    {
                        if (!mergeList.contains(batch) && batch.isResourceIncluded(contextResource.getCompleteKey()))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Context: {} shares a resource with {}: {}", new String[] { context, batch.getKey(), contextResource.getCompleteKey() });
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

    // If context batching is not enabled, then just add all the resources that would have been added in the context anyway.
    private Iterable<PluginResource> getUnbatchedResources(final Iterable<String> includedContexts, final WebResourceFilter filter)
    {
        LinkedHashSet<PluginResource> includedResources = new LinkedHashSet<PluginResource>();
        for (final String context : includedContexts)
        {
            Iterable<WebResourceModuleDescriptor> contextResources = dependencyResolver.getDependenciesInContext(context, skippedResources);

            for (final WebResourceModuleDescriptor contextResource : contextResources)
            {
                if (!allIncludedResources.contains(contextResource.getCompleteKey()))
                {
                    final List<PluginResource> moduleResources = pluginResourceLocator.getPluginResources(contextResource.getCompleteKey());
                    for (final PluginResource moduleResource : moduleResources)
                    {
                        if (filter.matches(moduleResource.getResourceName()))
                        {
                            includedResources.add(moduleResource);
                        }
                    }

                    allIncludedResources.add(contextResource.getCompleteKey());
                }
            }
        }

        return includedResources;
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
