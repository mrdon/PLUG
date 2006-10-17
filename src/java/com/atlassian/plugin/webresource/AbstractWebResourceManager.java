package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * A handy super-class that handles most of the resource management.
 * <p>
 * Sub-classes should implement {@link #getPluginAccessor()}, {@link #getRequestCache()} and
 * {@link #getRequestCacheKey()} and {@link #getResourceLinkFactory()} which are application specific
 */
public abstract class AbstractWebResourceManager implements WebResourceManager
{
    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String CSS_EXTENSION = ".css";

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
            return ;

        for (Iterator iterator = webResourceNames.iterator(); iterator.hasNext();)
        {
            String resourceName = (String) iterator.next();
            ModuleDescriptor descriptor = getPluginAccessor().getEnabledPluginModule(resourceName);
            if (descriptor == null)
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\".  Resource not found -->\n");
                continue;
            }
            if (!(descriptor instanceof WebResourceModuleDescriptor))
            {
                writer.write("<!-- Error loading resource \"" + descriptor + "\". Resource is not a WebResourceModule -->\n");
                continue;
            }

            for (Iterator iterator1 = descriptor.getResourceDescriptors().iterator(); iterator1.hasNext();)
            {
                ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator1.next();
                String name = resourceDescriptor.getName();
                String linkToResource = getResourceLinkFactory().getResourceLink(resourceDescriptor).getLinkToResource(descriptor, resourceDescriptor);
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
     * A factory for writing links to resources.  For example for some resources you may want to use the 'cached'
     * version, and for others, you may want to serve them dynamically.
     */
    protected abstract ResourceLinkFactory getResourceLinkFactory();

}
