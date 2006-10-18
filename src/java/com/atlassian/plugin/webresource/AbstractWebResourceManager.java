package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


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
public abstract class AbstractWebResourceManager implements WebResourceManager
{
    private Log log = LogFactory.getLog(AbstractWebResourceManager.class);
    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String CSS_EXTENSION = ".css";
    private static final String STATIC_RESOURCE_PREFIX = "/s/";
    private static final String STATIC_RESOURCE_SUFFIX = "/_";

    private static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;

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
        Map cache = getRequestCache();
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
        Collection webResourceNames = (Collection) getRequestCache().get(REQUEST_CACHE_RESOURCE_KEY);
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
        ModuleDescriptor descriptor = getPluginAccessor().getEnabledPluginModule(resourceName);
        if (descriptor == null)
        {
            writer.write("<!-- Error loading resource \"" + descriptor + "\".  Resource not found -->\n");
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
            String linkToResource = getStaticPluginResourcePrefix(descriptor, resourceDescriptor);
            if (name != null && name.endsWith(JAVA_SCRIPT_EXTENSION))
            {
                writer.write("<script type=\"text/javascript\" src=\"" + getBaseUrl() + linkToResource + "\"></script>\n");
            }
            else if (name != null && name.endsWith(CSS_EXTENSION))
            {
                writer.write("<link type=\"text/css\" rel=\"styleSheet\" media=\"all\" href=\"" + getBaseUrl() + linkToResource + "\" />\n");
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
        // "{base url}/s/{build num}/{system date}/_"
        return getBaseUrl() + STATIC_RESOURCE_PREFIX + getSystemBuildNumber() + "/" + getCacheFlushCounter() + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor)
    {
        return getStaticPrefix(moduleDescriptor.getPlugin()) + getPluginResourceUrl(moduleDescriptor, resourceDescriptor);
    }

    private String getPluginResourceUrl(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor)
    {
        return "/" + BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleDescriptor.getCompleteKey() + "/" + resourceDescriptor.getName();
    }

    private String getStaticPrefix(Plugin plugin)
    {
        String pluginVersion = plugin.getPluginInformation().getVersion();

        // "/s/{build num}/{plugin version}/{system date}/_"
        return STATIC_RESOURCE_PREFIX + getSystemBuildNumber() + "/" + pluginVersion + "/" + getCacheFlushCounter() + STATIC_RESOURCE_SUFFIX;
    }

    public void setIncludeMode(IncludeMode includeMode)
    {
        getRequestCache().put(REQUEST_CACHE_MODE_KEY, includeMode);
    }

    private IncludeMode getIncludeMode()
    {
        IncludeMode includeMode = (IncludeMode) getRequestCache().get(REQUEST_CACHE_MODE_KEY);
        if (includeMode == null)
            includeMode = DEFAULT_INCLUDE_MODE;
        return includeMode;
    }


    /**
     * Applications must implement this method to get access to the application's PluginAccessor
     */
    protected abstract PluginAccessor getPluginAccessor();

    /**
     * This must be a thread-local cache that will be accessable from both the page, and the decorator
     */
    protected abstract Map getRequestCache();

    /**
     * Represents the unique number, which when updated will flush the cache.  For most 'system-wide' resources,
     * this should be a number stored in the global application-properties.  For other resources (such as 'header' files
     * which contain colour changes), this should be a separate number
     *
     * @return A string representing the count
     */
    protected abstract String getCacheFlushCounter();

    /**
     * Represents the last time the system was updated.  This is generally obtained from BuildUtils or similar.
     */
    protected abstract String getSystemBuildNumber();

    /**
     * This should be the 'short' contextPath for the system.  In most cases, this would equal request.getContextPath()
     * (perhaps retrieved from a thread local), or else parsed from the application's base URL.
     */
    protected abstract String getBaseUrl();


}
