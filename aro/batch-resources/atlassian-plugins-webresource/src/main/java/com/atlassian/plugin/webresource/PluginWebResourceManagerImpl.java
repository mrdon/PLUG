package com.atlassian.plugin.webresource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.io.IOUtils;

import java.io.Writer;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;

public class PluginWebResourceManagerImpl implements PluginWebResourceManager
{
    private static final Log log = LogFactory.getLog(PluginWebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";

    private final WebResourceIntegration webResourceIntegration;
    private final List<WebResourceFormatter> webResourceFormatters;

    /**
     * Default constructor, creates with css and js web resource formatters.
     */
    public PluginWebResourceManagerImpl(WebResourceIntegration webResourceIntegration)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.webResourceFormatters = new ArrayList<WebResourceFormatter>();
        webResourceFormatters.add(new CssWebResourceFormatter());
        webResourceFormatters.add(new JavascriptWebResourceFormatter());
    }

    public PluginWebResourceManagerImpl(WebResourceIntegration webResourceIntegration,
                                        List<WebResourceFormatter> webResourceFormatters)
    {
        this.webResourceIntegration = webResourceIntegration;
        this.webResourceFormatters = webResourceFormatters;
    }

    public String getStaticResourcePrefix(String resourceCounter)
    {
        // "{base url}/s/{build num}/{system counter}/{resource counter}/_"
        return webResourceIntegration.getBaseUrl() + "/" +
                STATIC_RESOURCE_PREFIX + "/" +
                webResourceIntegration.getSystemBuildNumber() + "/" +
                webResourceIntegration.getSystemCounter() + "/" +
                resourceCounter + "/" +
                STATIC_RESOURCE_SUFFIX;
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

    public void writeRequiredResources(Writer writer, RequestMode mode)
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
            writeResource(resourceName, writer, mode);
        }
    }

    public void writeResource(String resourceName, Writer writer, RequestMode mode)
    {
        ModuleDescriptor descriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(resourceName);
        try
        {
            if (descriptor == null)
            {
                writer.write("<!-- Error loading resource \"" + resourceName + "\".  Resource not found -->\n");
                return;
            }
            else if (!(descriptor instanceof WebResourceModuleDescriptor))
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Resource is not a WebResourceModule -->\n");
                return;
            }

            WebResourceModuleDescriptor webResourceModuleDescriptor = (WebResourceModuleDescriptor) descriptor;
            if(RequestMode.BATCH.equals(mode))
            {
                batchResources(webResourceModuleDescriptor, writer);
            }
            else if(RequestMode.SINGLE.equals(mode))
            {
                singleResources(webResourceModuleDescriptor, writer);
            }
            else if(RequestMode.INLINE.equals(mode))
            {
                inlineResources(webResourceModuleDescriptor, writer);
            }
        }
        catch(IOException e)
        {
            log.error(e);
        }
    }

    private void inlineResources(WebResourceModuleDescriptor descriptor, Writer writer)
    {
        for (ResourceDescriptor resourceDescriptor : (List<ResourceDescriptor>) descriptor.getResourceDescriptors())
        {
            // todo handle web resources
            //if ("webContext".equalsIgnoreCase(resourceDescriptor.getParameter("source")))
            InputStream is = descriptor.getPlugin().getResourceAsStream(resourceDescriptor.getLocation());

            if (is == null)
            {
                log.error("Could not locate resource: " + resourceDescriptor.getLocation());
                continue;
            }

            try
            {
                IOUtils.copy(is, writer);
            }
            catch (IOException e)
            {
                log.error(e);
            }
        }
    }

    private void singleResources(WebResourceModuleDescriptor descriptor, Writer writer)
    {
        for (ResourceDescriptor resourceDescriptor : (List<ResourceDescriptor>) descriptor.getResourceDescriptors())
        {
            WebResourceFormatter webResourceFormatter = getWebResourceFormatter(resourceDescriptor.getName());
            try
            {
                if (webResourceFormatter != null)
                {
                    String urlPrefix = getResourceUrlPrefix(descriptor, resourceDescriptor);
                    String url = ResourceUrlFormatter.getResourceUrl(urlPrefix, descriptor.getCompleteKey(), resourceDescriptor.getName());
                    writer.write(webResourceFormatter.formatResource(url, resourceDescriptor.getParameters()));
                }
                else
                {
                    writer.write("<!-- Error loading resource \"" + descriptor + "\". Type " + resourceDescriptor.getType() + " is not handled -->\n");
                }
            }
            catch (IOException e)
            {
                log.error(e);
            }
        }
    }

    private void batchResources(WebResourceModuleDescriptor descriptor, Writer writer)
    {
        Set<WebResourceFormatter> formatters = new TreeSet<WebResourceFormatter>();

        for(ResourceDescriptor resourceDescriptor : (List<ResourceDescriptor>) descriptor.getResourceDescriptors())
        {
            WebResourceFormatter webResourceFormatter = getWebResourceFormatter(resourceDescriptor.getName());
            if (webResourceFormatter != null)
                formatters.add(webResourceFormatter);
            
            if(formatters.size() == webResourceFormatters.size())
                break; // early exit when we have all the web resource formatters
        }

        for(WebResourceFormatter webResourceFormatter : formatters)
        {
            try
            {
                String url = ResourceUrlFormatter.getBatchResourceUrl(getCachingUrlPrefix(descriptor),
                                                    descriptor.getCompleteKey(), webResourceFormatter.getExtension());
                writer.write(webResourceFormatter.formatResource(url, Collections.EMPTY_MAP));
            }
            catch (IOException e)
            {
                log.error(e);
            }
        }
    }

    private String getResourceUrlPrefix(WebResourceModuleDescriptor descriptor, ResourceDescriptor resourceDescriptor)
    {
        // non caching resource
        if("false".equalsIgnoreCase(resourceDescriptor.getParameter("cache")))
            return webResourceIntegration.getBaseUrl();

        return getCachingUrlPrefix(descriptor);
    }

    private String getCachingUrlPrefix(WebResourceModuleDescriptor descriptor)
    {
        return getStaticResourcePrefix(String.valueOf(descriptor.getPlugin().getPluginsVersion()));
    }

    private WebResourceFormatter getWebResourceFormatter(String name)
    {
        for (WebResourceFormatter webResourceFormatter : webResourceFormatters)
        {
            if (webResourceFormatter.matches(name))
            {
                return webResourceFormatter;
            }
        }
        return null;
    }
}
