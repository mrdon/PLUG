package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.*;


/**
 * A handy super-class that handles most of the resource management.
 * <p/>
 * To use this manager, you need to have the following UrlRewriteFilter code:
 * <pre>
 * &lt;rule>
        &lt;from>^/s/(.*)/_/(.*)&lt;/from>
        &lt;run class="com.atlassian.plugin.servlet.ResourceDownloadUtils" method="addCachingHeaders" />
        &lt;to type="forward">/$2&lt;/to>
    &lt;/rule>
 * </pre>
 *
 * Sub-classes should implement the abstract methods
 */
public class WebResourceManagerImpl implements WebResourceManager
{
    private Log log = LogFactory.getLog(WebResourceManagerImpl.class);
    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String CSS_EXTENSION = ".css";
    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;

    private final WebResourceIntegration webResourceIntegration;

    public WebResourceManagerImpl(WebResourceIntegration webResourceIntegration)
    {
        this.webResourceIntegration = webResourceIntegration; //constructor for JIRA / Pico
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
            webResourceNames = new ArrayList(); // todo - need a set to ensure uniqueness, but we need to also have ordering

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
            return;

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
            String linkToResource = getStaticPluginResourcePrefix(descriptor, resourceDescriptor.getName());
            if (name != null && name.endsWith(JAVA_SCRIPT_EXTENSION))
            {
                writer.write("<script type=\"text/javascript\" src=\"" + linkToResource + "\"></script>\n");
            }
            else if (name != null && name.endsWith(CSS_EXTENSION))
            {
                writer.write("<link type=\"text/css\" rel=\"styleSheet\" media=\"all\" href=\"" + linkToResource + "\" />\n");
            }
            else
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Type " + resourceDescriptor.getType() + " is not handled -->\n");
            }
        }

/*
    This is half-finished code to allow linking to resources in *any* module, not just web resources
        for (Iterator iterator = webResourceNames.iterator(); iterator.hasNext();)
        {
            String resourceName = (String) iterator.next();
            if (resourceName == null)
                continue;

            String pluginKey = resourceName.substring(0, resourceName.indexOf(":"));
            String resourceKey = resourceName.substring(resourceName.indexOf(":") + 1);
            Plugin plugin = getPluginAccessor().getPlugin(pluginKey);
            String linkToResource = getStaticPrefix(plugin) + "/" + BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX + "/" +
                    resourceKey + "/" + resourceKey;
            if (resourceName.endsWith(JAVA_SCRIPT_EXTENSION))
            {
                writer.write("<script type=\"text/javascript\" src=\"" + contextPath + linkToResource + "\"></script>\n");
            }
            else if (resourceName.endsWith(CSS_EXTENSION))
            {
                writer.write("<link type=\"text/css\" rel=\"styleSheet\" media=\"all\" href=\"" + contextPath + linkToResource + "\" />\n");
            }
            else
            {
                writer.write("<!-- Error loading resource. Type " + resourceName + " is not handled -->\n");
            }
        }
*/

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
     * @deprecated
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        return getStaticPluginResource(moduleDescriptor, resourceName);
    }

    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName)
    {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        String prefix = getStaticResourcePrefix(moduleDescriptor.getPlugin().getPluginInformation().getVersion());

        // "/download/resources/plugin.key:module.key/resource.name"
        String suffix = "/" + BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleDescriptor.getCompleteKey() + "/" + resourceName;
        return prefix + suffix;
    }

    public String getStaticPluginResource(String pluginModuleKey, String resourceName)
    {
        return getStaticPluginResource(webResourceIntegration.getPluginAccessor().getEnabledPluginModule(pluginModuleKey), resourceName);
    }

    public void setIncludeMode(IncludeMode includeMode)
    {
        webResourceIntegration.getRequestCache().put(REQUEST_CACHE_MODE_KEY, includeMode);
    }

    private IncludeMode getIncludeMode()
    {
        IncludeMode includeMode = (IncludeMode) webResourceIntegration.getRequestCache().get(REQUEST_CACHE_MODE_KEY);
        if (includeMode == null)
            includeMode = DEFAULT_INCLUDE_MODE;
        return includeMode;
    }



}
