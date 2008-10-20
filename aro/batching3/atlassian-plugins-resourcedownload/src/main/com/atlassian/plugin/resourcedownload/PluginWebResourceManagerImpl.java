package com.atlassian.plugin.resourcedownload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.set.ListOrderedSet;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.io.Writer;
import java.io.IOException;
import java.io.StringWriter;

import com.atlassian.plugin.resourcedownload.formatter.WebResourceFormatter;

public class PluginWebResourceManagerImpl implements PluginWebResourceManager
{
    private static final Log log = LogFactory.getLog(PluginWebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.batch.webresource.names";

    private final WebResourceIntegration webResourceIntegration;
    private final PluginResourceLocator pluginResourceLocator;
    private final List<WebResourceFormatter> webResourceFormatters;

    public PluginWebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration,
        List<WebResourceFormatter> webResourceFormatters)
    {
        this.pluginResourceLocator = pluginResourceLocator;
        this.webResourceIntegration = webResourceIntegration;
        this.webResourceFormatters = webResourceFormatters;
    }

    public void requireResource(String resourceName)
    {
        Map cache = webResourceIntegration.getRequestCache();
        Collection webResourceNames = (Collection) cache.get(REQUEST_CACHE_RESOURCE_KEY);
        if (webResourceNames == null)
        {
            webResourceNames = new ListOrderedSet();
        }

        webResourceNames.add(resourceName);
        cache.put(REQUEST_CACHE_RESOURCE_KEY, webResourceNames);
    }

    public void writeRequiredResources(Writer writer)
    {
        Collection webResourceNames = (Collection) webResourceIntegration.getRequestCache().get(REQUEST_CACHE_RESOURCE_KEY);
        if (webResourceNames == null || webResourceNames.isEmpty())
        {
            log.debug("No resources required to write");
            return;
        }

        for (Object webResourceName : webResourceNames)
        {
            String resourceName = (String) webResourceName;
            writeResourceTags(resourceName, writer);
        }
    }

    public void writeResourceTags(String resourceName, Writer writer)
    {
        List<Resource> resources = pluginResourceLocator.locateByCompleteKey(resourceName);
        if(resources == null)
        {
            //todo
            return;
        }

        for(Resource resource : resources)
        {
            WebResourceFormatter formatter = getWebResourceFormatter(resource.getResourceName());
            if(formatter == null)
            {
                //todo
                return;
            }

            try
            {

                writer.write(formatter.formatResource(resource.getUrl(), resource.getParams()));
            }
            catch (IOException e)
            {
                log.error(e);
            }
        }
    }

    private WebResourceFormatter getWebResourceFormatter(String resourceName)
    {
        for(WebResourceFormatter webResourceFormatter : webResourceFormatters)
        {
            if(webResourceFormatter.matches(resourceName))
                return webResourceFormatter;
        }
        return null;
    }

    public String getRequiredResources()
    {
        StringWriter writer = new StringWriter();
        writeRequiredResources(writer);
        return writer.toString();
    }

    public String getResourceTags(String resourceName)
    {
        StringWriter writer = new StringWriter();
        writeResourceTags(resourceName, writer);
        return writer.toString();
    }
}
