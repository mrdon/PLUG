package com.atlassian.plugin.webresource;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.webresource.batch.BatchResource;
import com.atlassian.plugin.webresource.batch.Batched;
import com.atlassian.plugin.webresource.PluginResource;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.PluginResourceDownload;
import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor implements Batched
{
    private static String[] BATCH_PARAMS = new String[] { "ieonly", "media" };

    /**
     * As this descriptor just handles resources, you should never call this
     */
    public Object getModule()
    {
        throw new UnsupportedOperationException("There is no module for Web Resources");
    }

    public List<PluginResource> getPluginResources(BatchResource batchResource)
    {
        List<PluginResource> pluginResources = new ArrayList<PluginResource>();
        for(ResourceDescriptor resource : (List<ResourceDescriptor>) getResourceDescriptors(PluginResourceDownload.DOWNLOAD_RESOURCE))
        {
            if(matches(resource, batchResource))
            {
                pluginResources.add(new PluginResource(getCompleteKey(), resource.getName()));
            }
        }
        return pluginResources;
    }

    public List<BatchResource> getBatchResources()
    {
        List<BatchResource> batchResources = new ArrayList<BatchResource>();
        for(ResourceDescriptor resourceDescriptor : (List<ResourceDescriptor>) getResourceDescriptors(PluginResourceDownload.DOWNLOAD_RESOURCE))
        {
            BatchResource batchResource = createBatchResource(resourceDescriptor);
            if(!batchResources.contains(batchResource))
                batchResources.add(batchResource);
        }
        return batchResources;
    }

    private boolean matches(ResourceDescriptor resourceDescriptor, BatchResource batchResource)
    {
        if(!resourceDescriptor.getName().endsWith("." + batchResource.getType()))
            return false;

        for(Map.Entry<String, String> entry : batchResource.getParams().entrySet())
        {
            if(!entry.getValue().equals(resourceDescriptor.getParameter(entry.getKey())))
                return false;
        }
        return true;
    }

    private BatchResource createBatchResource(ResourceDescriptor resourceDescriptor)
    {
        String name = resourceDescriptor.getName();
        String type = name.substring(name.lastIndexOf(".") + 1);

        Map<String, String> params = new TreeMap<String, String>();
        for(String param : BATCH_PARAMS) // todo get batch params depending on type
        {
            String value = resourceDescriptor.getParameter(param);
            if(StringUtils.isNotBlank(value))
                params.put(param, value);
        }

        return new BatchResource(type, getCompleteKey(), params);
    }
}
