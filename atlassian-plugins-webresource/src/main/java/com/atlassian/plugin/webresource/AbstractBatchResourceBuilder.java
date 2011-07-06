package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Resources;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.PluginResourceLocator.BATCH_PARAMS;
import static com.google.common.collect.Iterables.filter;

/**
 * This class is used to add individual resources into a larger batch resource.
 */
public abstract class AbstractBatchResourceBuilder implements DownloadableResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(AbstractBatchResourceBuilder.class);
    private static final String RESOURCE_SOURCE_PARAM = "source";
    private static final String RESOURCE_BATCH_PARAM = "batch";
    private static final String DOWNLOAD_TYPE = "download";

    private final PluginAccessor pluginAccessor;
    private final WebResourceUrlProvider webResourceUrlProvider;
    protected DownloadableResourceFinder resourceFinder;

    public AbstractBatchResourceBuilder(PluginAccessor pluginAccessor, WebResourceUrlProvider webResourceUrlProvider, DownloadableResourceFinder resourceFinder)
    {
        this.pluginAccessor = pluginAccessor;
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.resourceFinder = resourceFinder;
    }

    protected List<DownloadableResource> resolve(String moduleKey, String batchType, Map<String, String> batchParams)
    {
        List<DownloadableResource> resources = new ArrayList<DownloadableResource>();
        final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleKey);
        if (moduleDescriptor == null)
        {
            log.info("Resource batching configuration refers to plugin that does not exist: " + moduleKey);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("searching resources in: " + moduleKey);
            }

            for (final ResourceDescriptor resourceDescriptor : filter(moduleDescriptor.getResourceDescriptors(), new Resources.TypeFilter(DOWNLOAD_TYPE)))
            {
                if (isResourceInBatch(resourceDescriptor, batchType, batchParams))
                {
                    DownloadableResource downloadableResource = resourceFinder.find(moduleDescriptor.getCompleteKey(), resourceDescriptor.getName());
                    if (RelativeURLTransformResource.matches(resourceDescriptor))
                    {
                        downloadableResource = new RelativeURLTransformResource(webResourceUrlProvider, moduleDescriptor, downloadableResource);
                    }

                    resources.add(downloadableResource);
                }
            }
        }

        return resources;
    }

    protected DownloadableResourceFinder getResourceFinder()
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

    private boolean skipBatch(final ResourceDescriptor resourceDescriptor)
    {
        // you can't batch forwarded requests
        return "false".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_BATCH_PARAM))
            || "webContext".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_SOURCE_PARAM));
    }
}
