package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

/**
 * A handy super-class that handles most of the resource management.
 * <p/>
 * To use this manager, you need to have the following UrlRewriteFilter code:
 * <pre>
 * &lt;rule>
 * &lt;from>^/s/(.*)/_/(.*)&lt;/from>
 * &lt;run class="com.atlassian.plugin.servlet.ResourceDownloadUtils" method="addCachingHeaders" />
 * &lt;to type="forward">/$2&lt;/to>
 * &lt;/rule>
 * </pre>
 * <p/>
 * Sub-classes should implement the abstract methods
 */
public class WebResourceManagerImpl implements WebResourceManager
{
    private static final Log log = LogFactory.getLog(WebResourceManagerImpl.class);

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;

    protected final WebResourceIntegration webResourceIntegration;
    protected final PluginResourceLocator pluginResourceLocator;
    protected static final List<WebResourceFormatter> webResourceFormatters = Arrays.< WebResourceFormatter>asList(new CssWebResourceFormatter(), new JavascriptWebResourceFormatter());

    public WebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration)
    {
        this.pluginResourceLocator = pluginResourceLocator;
        this.webResourceIntegration = webResourceIntegration;
    }

    public void requireResource(String resourceName)
    {
        log.info("Requiring resource: " + resourceName);
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
            log.info("No resources required to write");
            return;
        }

        for (Object webResourceName : webResourceNames)
        {
            String resourceName = (String) webResourceName;
            writeResourceTags(resourceName, writer);
        }
    }

    public String getRequiredResources()
    {
        StringWriter writer = new StringWriter();
        writeRequiredResources(writer);
        return writer.toString();
    }

    public void writeResourceTags(String resourceName, Writer writer)
    {
        List<PluginResource> resources = pluginResourceLocator.getPluginResource(resourceName);
        if(resources == null)
        {
            writeContentAndSwallowErrors("<!-- Error loading resource \"" + resourceName + "\".  Resource not found -->\n", writer);
            return;
        }

        for(PluginResource resource : resources)
        {
            WebResourceFormatter formatter = getWebResourceFormatter(resource.getResourceName());
            if(formatter == null)
            {
                writeContentAndSwallowErrors("<!-- Error loading resource \"" + resourceName + "\".  Resource formatter not found -->\n", writer);
                continue;
            }

            writeContentAndSwallowErrors(formatter.formatResource(resource.getUrl(), resource.getParams()), writer);
        }
    }

    public String getResourceTags(String resourceName)
    {
        StringWriter writer = new StringWriter();
        writeResourceTags(resourceName, writer);
        return writer.toString();
    }

    private void writeContentAndSwallowErrors(String content, Writer writer)
    {
        try
        {
            writer.write(content);
        }
        catch (IOException e)
        {
            log.error(e);
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

    public String getStaticResourcePrefix()
    {
        return pluginResourceLocator.getStaticResourceUrlPrefix();
    }

    public String getStaticResourcePrefix(String resourceCounter)
    {
        return pluginResourceLocator.getStaticResourceUrlPrefix(resourceCounter);
    }

    public String getStaticPluginResource(String moduleCompleteKey, String resourceName)
    {
        return pluginResourceLocator.getStaticResourceUrl(moduleCompleteKey, resourceName);
    }

    /* Deprecated methods */

    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor.getCompleteKey(), resourceName);
    }

    /**
     * @deprecated Since 2.2. Use {@link #writeRequiredResources} instead.
     */
    public void includeResources(Writer writer)
    {
        writeRequiredResources(writer);
    }

    /**
     * @deprecated Since 2.2. Use #writeResourceTags instead.
     */
    public void requireResource(String resourceName, Writer writer)
    {
        writeResourceTags(resourceName, writer);
    }

    /**
     * @deprecated Use {@link #getStaticPluginResource(com.atlassian.plugin.ModuleDescriptor, String)} instead
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName);
    }

    /**
     * @deprecated Since 2.2.
     */
    public void setIncludeMode(IncludeMode includeMode)
    {
        webResourceIntegration.getRequestCache().put(REQUEST_CACHE_MODE_KEY, includeMode);
    }

    IncludeMode getIncludeMode()
    {
        IncludeMode includeMode = (IncludeMode) webResourceIntegration.getRequestCache().get(REQUEST_CACHE_MODE_KEY);
        if (includeMode == null)
        {
            includeMode = DEFAULT_INCLUDE_MODE;
        }
        return includeMode;
    }
}
