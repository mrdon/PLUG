package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.webresource.ContextBatchPluginResource.CONTEXT_SEPARATOR;
import static com.atlassian.plugin.webresource.ResourceUtils.getType;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

import com.atlassian.plugin.ModuleDescriptor;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
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
    private static Logger log = LoggerFactory.getLogger(ContextBatch.class);
    private static final String UTF8 = "UTF-8";
    private static final String MD5 = "MD5";
    private static final Ordering<ModuleDescriptor<?>> MODULE_KEY_ORDERING = Ordering.natural().onResultOf(new TransformDescriptorToKey());
    private final String tempPath;

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
        final Iterable<WebResourceModuleDescriptor> resources = concat(b1.getResources(), b2.getResources());
        // Merge assumes that the merged batched doesn't overlap with the current one.
        final Iterable<PluginResourceBatchParams> params = concat(b1.getResourceParams(), b2.getResourceParams());
        return new ContextBatch(key, contexts, resources, params,b1.tempPath);
    }

    private final String key;
    private final Iterable<String> contexts;
    private final Iterable<WebResourceModuleDescriptor> resources;
    private final Iterable<String> resourceKeys;
    private final Set<PluginResourceBatchParams> resourceParams;

    ContextBatch(final String context, final Iterable<WebResourceModuleDescriptor> resources,String temp)
    {
        this(context, ImmutableList.of(context), resources, ImmutableList.<PluginResourceBatchParams> of(),temp);
    }

    ContextBatch(final String key, final Iterable<String> contexts, final Iterable<WebResourceModuleDescriptor> resources, final Iterable<PluginResourceBatchParams> resourceParams,String temp)
    {
        this.key = key;
        this.contexts = copyOf(contexts);
        this.resourceParams = newHashSet(resourceParams);
        this.tempPath = temp;

        // Note: Ordering is important in producing a consistent hash.
        // But, dependency order is not important when producing the PluginResource,
        // it is only important when we serve the resource. So it is safe to reorder
        // this.
        this.resources = ImmutableSortedSet.copyOf(MODULE_KEY_ORDERING, resources);
        // A convenience object to make searching easier
        this.resourceKeys =  transform(resources, new TransformDescriptorToKey());
    }

    boolean isResourceIncluded(final String resourceModuleKey)
    {
        return contains(resourceKeys, resourceModuleKey);
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
        final String hash = createHash();
        return transform(resourceParams, new Function<PluginResourceBatchParams, PluginResource>()
        {
            public PluginResource apply(final PluginResourceBatchParams param)
            {
                return new ContextBatchPluginResource(key, contexts, hash, param.getType(), param.getParameters(),tempPath);
            }
        });
    }

    private String createHash()
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            for (WebResourceModuleDescriptor moduleDescriptor : resources)
            {
                String version = moduleDescriptor.getPlugin().getPluginInformation().getVersion();
                md5.update(moduleDescriptor.getCompleteKey().getBytes(UTF8));
                md5.update(version.getBytes(UTF8));
            }

            return new String(Hex.encodeHex(md5.digest()));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AssertionError("MD5 hashing algorithm is not available.");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError("UTF-8 encoding is not available.");
        }
    }

    String getKey()
    {
        return key;
    }

    Iterable<String> getContexts()
    {
        return contexts;
    }

    Iterable<WebResourceModuleDescriptor> getResources()
    {
        return resources;
    }

    Iterable<PluginResourceBatchParams> getResourceParams()
    {
        return Collections.unmodifiableSet(resourceParams);
    }
}
