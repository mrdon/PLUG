package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * A handy super-class that handles most of the resource management.
 * <p/>
 * Sub-classes should implement the abstract methods
 */
public abstract class AbstractWebResourceManager implements WebResourceManager
{
    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String CSS_EXTENSION = ".css";
    private static final String STATIC_RESOURCE_PREFIX = "/s/";
    private static final String STATIC_RESOURCE_SUFFIX = "/c";

    public void requireResource(String resourceName)
    {
        Map cache = getRequestCache();
        Collection webResourceNames = (Collection) cache.get(getRequestCacheKey());
        if (webResourceNames == null)
            webResourceNames = new ArrayList(); // todo - need a set to ensure uniqueness, but we need to also have ordering

        webResourceNames.add(resourceName);
        cache.put(getRequestCacheKey(), webResourceNames);
    }

    public void includeResources(String contextPath, Writer writer) throws IOException
    {
        Collection webResourceNames = (Collection) getRequestCache().get(getRequestCacheKey());
        if (webResourceNames == null || webResourceNames.isEmpty())
            return;

        for (Iterator iterator = webResourceNames.iterator(); iterator.hasNext();)
        {
            String resourceName = (String) iterator.next();
            ModuleDescriptor descriptor = getPluginAccessor().getEnabledPluginModule(resourceName);
            if (descriptor == null)
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\".  Resource not found -->\n");
                continue;
            }
            else if (!(descriptor instanceof WebResourceModuleDescriptor))
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Resource is not a WebResourceModule -->\n");
                continue;
            }

            for (Iterator iterator1 = descriptor.getResourceDescriptors().iterator(); iterator1.hasNext();)
            {
                ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator1.next();
                String name = resourceDescriptor.getName();
                String linkToResource = getStaticPluginResourcePrefix(descriptor, resourceDescriptor);
                if (name != null && name.endsWith(JAVA_SCRIPT_EXTENSION))
                {
                    writer.write("<script type=\"text/javascript\" src=\"" + contextPath + linkToResource + "\"></script>\n");
                }
                else if (name != null && name.endsWith(CSS_EXTENSION))
                {
                    writer.write("<link type=\"text/css\" rel=\"styleSheet\" media=\"all\" href=\"" + contextPath + linkToResource + "\" />\n");
                }
                else
                {
                    writer.write("<!-- Error loading resource \"" + descriptor + "\". Type " + resourceDescriptor.getType() + " is not handled -->\n");
                }
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
        // "/s/{build num}/{system date}/c"
        return STATIC_RESOURCE_PREFIX + getSystemBuildNumber() + "/" + getCacheFlushDate() + STATIC_RESOURCE_SUFFIX;
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

        // "/s/{build num}/{plugin version}/{system date}/c"
        return STATIC_RESOURCE_PREFIX + getSystemBuildNumber() + "/" + pluginVersion + "/" + getCacheFlushDate() + STATIC_RESOURCE_SUFFIX;
    }


    /**
     * Applications must implement this method to get access to the applications PluginAccessor
     */
    protected abstract PluginAccessor getPluginAccessor();

    /**
     * This must be a thread-local cache that will be accessable from both the page, and the decorator
     */
    protected abstract Map getRequestCache();

    /**
     * The key to use when getting and putting objects into {@link #getRequestCache()}
     */
    protected abstract String getRequestCacheKey();

    /**
     * Represents the last date at which the cache should be flushed.  For most 'system-wide' resources,
     * this should be a date stored in the global application-properties.  For other resources (such as 'header' files
     * which contain colour changes), this should be the date of the last update.
     *
     * @return A string representing the date, which can be of format "yyMMddHHmmssZ".
     */
    protected abstract String getCacheFlushDate();

    /**
     * Represents the last time the system was updated.  This is generally obtained from BuildUtils or similar.
     */
    protected abstract String getSystemBuildNumber();


}
