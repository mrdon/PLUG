package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

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

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;
    private static final WebResourceFormatter[] WEB_RESOURCE_FORMATTERS = new WebResourceFormatter[] {
        new CssWebResourceFormatter(),
        new JavascriptWebResourceFormatter(),
    };

    private final WebResourceIntegration webResourceIntegration;

    public WebResourceManagerImpl(WebResourceIntegration webResourceIntegration)
    {
        this.webResourceIntegration = webResourceIntegration; //constructor for JIRA / Pico
    }

    public void requireResource(String resourceName)
    {
        if (WebResourceManager.DELAYED_INCLUDE_MODE.equals(getIncludeMode()))
        {
            requireDelayedResource(resourceName);
        }
        else
        {
            throw new IllegalStateException("Require Writer for Inline mode.");
        }
    }

    public void requireResource(String resourceName, Writer writer)
    {
        if (WebResourceManager.DELAYED_INCLUDE_MODE.equals(getIncludeMode()))
        {
            requireDelayedResource(resourceName);
        }
        else
        {
            try
            {
                includeResource(resourceName, writer);
            }
            catch (IOException e)
            {
                log.error(e);
            }
        }
    }

    private void requireDelayedResource(String resourceName)
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

    public void includeResources(Writer writer)
    {
        try
        {
            if (WebResourceManager.DELAYED_INCLUDE_MODE.equals(getIncludeMode()))
            {
                includeDelayedResources(writer);
            }
        }
        catch (IOException e)
        {
            log.error(e);
        }

    }

    private void includeDelayedResources(Writer writer) throws IOException
    {
        Collection webResourceNames = (Collection) webResourceIntegration.getRequestCache().get(REQUEST_CACHE_RESOURCE_KEY);
        if (webResourceNames == null || webResourceNames.isEmpty())
        {
            return;
        }

        for (Iterator iterator = webResourceNames.iterator(); iterator.hasNext();)
        {
            String resourceName = (String) iterator.next();
            includeResource(resourceName, writer);
        }
    }

    private void includeResource(String resourceName, Writer writer) throws IOException
    {
        ModuleDescriptor descriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(resourceName);
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

        for (Iterator iterator1 = descriptor.getResourceDescriptors().iterator(); iterator1.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator1.next();
            String name = resourceDescriptor.getName();
            final String linkToResource;
            if ("false".equalsIgnoreCase(resourceDescriptor.getParameter("cache")))
            {
                linkToResource = webResourceIntegration.getBaseUrl() + getResourceUrl(descriptor, name);
            }
            else
            {
                linkToResource = getStaticPluginResource(descriptor, name);
            }

            WebResourceFormatter webResourceFormatter = getWebResourceFormatter(name);
            if(webResourceFormatter != null)
            {
                writer.write(webResourceFormatter.formatResource(name, linkToResource, resourceDescriptor.getParameters()));
            }
            else
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Type " + resourceDescriptor.getType() + " is not handled -->\n");
            }
        }
    }

    private WebResourceFormatter getWebResourceFormatter(String name)
    {
        for (int i = 0; i < WEB_RESOURCE_FORMATTERS.length; i++)
        {
            WebResourceFormatter webResourceFormatter = WEB_RESOURCE_FORMATTERS[i];
            if(webResourceFormatter.matches(name))
            {
                return webResourceFormatter;
            }
        }
        return null;
    }

    public String getStaticResourcePrefix()
    {
        // "{base url}/s/{build num}/{system counter}/_"
        return webResourceIntegration.getBaseUrl() + "/" +
                STATIC_RESOURCE_PREFIX + "/" +
                webResourceIntegration.getSystemBuildNumber() + "/" +
                webResourceIntegration.getSystemCounter() + "/" +
                STATIC_RESOURCE_SUFFIX;
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

    /**
     * @deprecated Use {@link #getStaticPluginResource(com.atlassian.plugin.ModuleDescriptor, String)} instead
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName);
    }

    /**
     * @return "{base url}/s/{build num}/{system counter}/{plugin version}/_/download/resources/{plugin.key:module.key}/{resource.name}"
     */
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        String prefix = getStaticResourcePrefix(moduleDescriptor.getPlugin().getPluginInformation().getVersion());

        // "/download/resources/plugin.key:module.key/resource.name"
        String suffix = getResourceUrl(moduleDescriptor, resourceName);
        return prefix + suffix;
    }

    // "/download/resources/plugin.key:module.key/resource.name"
    private String getResourceUrl(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        return "/" + AbstractFileServerServlet.SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleDescriptor.getCompleteKey() + "/" + resourceName;
    }

    public String getStaticPluginResource(String pluginModuleKey, String resourceName)
    {
        return getStaticPluginResource(webResourceIntegration.getPluginAccessor().getEnabledPluginModule(pluginModuleKey), resourceName);
    }

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
