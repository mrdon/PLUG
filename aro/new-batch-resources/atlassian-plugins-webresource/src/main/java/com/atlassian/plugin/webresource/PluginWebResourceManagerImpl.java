package com.atlassian.plugin.webresource;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Collection;
import java.util.List;

import com.atlassian.plugin.webresource.batch.BatchResource;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceDescriptor;

public class PluginWebResourceManagerImpl implements PluginWebResourceManager
{
    private static final Log log = LogFactory.getLog(PluginWebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.batch.webresource.names";

    private final WebResourceIntegration webResourceIntegration;
    private final List<WebResourceFormatter> webResourceFormatters;

    public PluginWebResourceManagerImpl(WebResourceIntegration webResourceIntegration, List<WebResourceFormatter> webResourceFormatters)
    {
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

    //todo
    public void writeResourceContent(String resourceName, Writer writer)
    {
//        WebResourceModuleDescriptor descriptor = getWebResourceModuleDescriptor(resourceName, writer);
    }

    public void writeResourceTags(String resourceName, Writer writer)
    {
        WebResourceModuleDescriptor moduleDescriptor = getWebResourceModuleDescriptor(resourceName, writer);
        if(moduleDescriptor == null)
            return;

        if (!isBatchingMode())
        {
            serveIndiviualResources(resourceName, writer, moduleDescriptor);
            return;
        }

        List<BatchResource> batches = moduleDescriptor.getBatchResources();
        for(BatchResource batchResource : batches)
        {
            WebResourceFormatter formatter = getWebResourceFormatter("foo." + batchResource.getType()); //todo
            if(formatter != null)
            {
                String url = getStaticPluginResourcePrefix(moduleDescriptor.getPlugin()) + batchResource.getUrl();
                formatTag(url, batchResource.getParams(), formatter, writer);
            }
            else
            {
                try
                {
                    writer.write("<!-- Error loading resource formatter for \"" + batchResource.getModuleCompleteKey() + "\". Type " + batchResource.getType() + " is not handled -->\n");
                }
                catch (IOException e)
                {
                    log.error(e);
                }
            }
        }
    }

    private void serveIndiviualResources(String resourceName, Writer writer, WebResourceModuleDescriptor moduleDescriptor)
    {
        for (ResourceDescriptor resourceDescriptor : (List<ResourceDescriptor>)moduleDescriptor.getResourceDescriptors())
        {
            WebResourceFormatter formatter = getWebResourceFormatter(resourceDescriptor.getName());
            if(formatter != null)
            {
                PluginResource pluginResource = new PluginResource(resourceName, resourceDescriptor.getName());

                String url = "";
                if (!"false".equalsIgnoreCase(resourceDescriptor.getParameter("cache"))) //todo preserve for batching
                    url += getStaticPluginResourcePrefix(moduleDescriptor.getPlugin());

                url += pluginResource.getUrl();
                formatTag(url, resourceDescriptor.getParameters(), formatter, writer);
            }
            else
            {
                try
                {
                    writer.write("<!-- Error loading resource formatter for \"" + resourceName + " -->\n");
                }
                catch (IOException e)
                {
                    log.error(e);
                }
            }
        }
    }

    private void formatTag(String url, Map<String, String> params, WebResourceFormatter formatter, Writer writer)
    {
        try
        {
            writer.write(formatter.format(url, params));
        }
        catch (IOException e)
        {
            log.error(e);
        }
    }

    private String getStaticPluginResourcePrefix(Plugin plugin)
    {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        return webResourceIntegration.getBaseUrl() + "/" +
                STATIC_RESOURCE_PREFIX + "/" +
                webResourceIntegration.getSystemBuildNumber() + "/" +
                webResourceIntegration.getSystemCounter() + "/" +
                plugin.getPluginsVersion() + "/" +
                STATIC_RESOURCE_SUFFIX;
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

    private WebResourceModuleDescriptor getWebResourceModuleDescriptor(String moduleCompleteKey, Writer writer)
    {
        ModuleDescriptor descriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleCompleteKey);
        try
        {
            if (descriptor == null)
            {
                writer.write("<!-- Error loading resource \"" + moduleCompleteKey + "\".  Resource not found -->\n");
            }
            else if (!(descriptor instanceof WebResourceModuleDescriptor))
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Resource is not a WebResourceModule -->\n");
            }
            else
            {
                return (WebResourceModuleDescriptor) descriptor;
            }
        }
        catch(IOException e)
        {
            log.error(e);
        }
        return null;
    }

    //todo
    private boolean isBatchingMode()
    {
        return true;
    }

    public String getRequiredResources()
    {
        StringWriter writer = new StringWriter();
        writeRequiredResources(writer);
        return writer.toString();
    }

    public String getResourceContent(String resourceName)
    {
        StringWriter writer = new StringWriter();
        writeResourceContent(resourceName,writer);
        return writer.toString();
    }

    public String getResourceTags(String resourceName)
    {
        StringWriter writer = new StringWriter();
        writeResourceTags(resourceName, writer);
        return writer.toString();
    }
}
