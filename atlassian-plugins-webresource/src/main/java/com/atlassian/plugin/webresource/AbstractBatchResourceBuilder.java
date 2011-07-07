package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.PluginResourceLocator.BATCH_PARAMS;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.emptyList;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Resources.TypeFilter;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Map;

/**
 * This class is used to add individual resources into a larger batch resource.
 */
abstract class AbstractBatchResourceBuilder implements DownloadableResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(AbstractBatchResourceBuilder.class);
    private static final String RESOURCE_SOURCE_PARAM = "source";
    private static final String RESOURCE_BATCH_PARAM = "batch";
    private static final String DOWNLOAD_TYPE = "download";

    private final PluginAccessor pluginAccessor;
    private final WebResourceUrlProvider webResourceUrlProvider;
    protected DownloadableResourceFinder resourceFinder;

    AbstractBatchResourceBuilder(final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder)
    {
        this.pluginAccessor = pluginAccessor;
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.resourceFinder = resourceFinder;
    }

    Iterable<DownloadableResource> resolve(final String moduleKey, final String batchType, final Map<String, String> batchParams)
    {
        final ModuleDescriptor<?> desc = pluginAccessor.getEnabledPluginModule(moduleKey);
        if (desc == null)
        {
            log.info("Resource batching configuration refers to plugin that does not exist: {}", moduleKey);
            return emptyList();
        }
        log.debug("searching resources in: {}", moduleKey);

        final Iterable<ResourceDescriptor> downloadDescriptors = filter(desc.getResourceDescriptors(), new TypeFilter(DOWNLOAD_TYPE));
        final Iterable<ResourceDescriptor> inBatch = filter(downloadDescriptors, new Predicate<ResourceDescriptor>()
        {
            public boolean apply(final ResourceDescriptor resourceDescriptor)
            {
                return isResourceInBatch(resourceDescriptor, batchType, batchParams);
            }
        });
        final Iterable<DownloadableResource> resources = transform(inBatch, new Function<ResourceDescriptor, DownloadableResource>()
        {
            public DownloadableResource apply(final ResourceDescriptor from)
            {
                final DownloadableResource result = resourceFinder.find(desc.getCompleteKey(), from.getName());
                if (result != null && RelativeURLTransformResource.matches(from))
                {
                    return new RelativeURLTransformResource(webResourceUrlProvider, desc, result);
                }
                return result;
            }
        });

        return filter(resources, notNull());
    }

    DownloadableResourceFinder getResourceFinder()
    {
        return resourceFinder;
    }

    private boolean isResourceInBatch(final ResourceDescriptor resourceDescriptor, final String batchType, final Map<String, String> batchParams)
    {
        if (!descriptorTypeMatchesResourceType(resourceDescriptor, batchType))
        {
            return false;
        }

        if (skipBatch(resourceDescriptor))
        {
            return false;
        }

        for (final String param : BATCH_PARAMS)
        {
            final String batchValue = batchParams.get(param);
            final String resourceValue = resourceDescriptor.getParameter(param);

            if ((batchValue == null) && (resourceValue != null))
            {
                return false;
            }

            if ((batchValue != null) && !batchValue.equals(resourceValue))
            {
                return false;
            }
        }
        return true;
    }

    private boolean descriptorTypeMatchesResourceType(final ResourceDescriptor resourceDescriptor, final String type)
    {
        return endsWith(resourceDescriptor.getName(), ".", type);
    }

    static boolean skipBatch(final ResourceDescriptor resourceDescriptor)
    {
        // you can't batch forwarded requests
        final boolean doNotBatch = "false".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_BATCH_PARAM));
        return doNotBatch || "webContext".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_SOURCE_PARAM));
    }
}
