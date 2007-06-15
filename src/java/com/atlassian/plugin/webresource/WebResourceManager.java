package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.io.Writer;
import java.io.IOException;

/**
 * Manage 'css', 'javascript' and other 'resources' that are usually linked at the top of pages using
 * <code>&lt;script&gt;</code> and <code>&lt;link&gt; tags.
 * <p>
 * By using the WebResourceManager, components can declare dependencies on javascript and css that they would
 * otherwise have to embed inline (which means that it can't be cached, and are often duplicated in a page).
 */
public interface WebResourceManager
{
    /**
     * Called by a component to indicate that a certain resource is required to be inserted into
     * this page.  Note that this will always include the resources as if we are in {@link #DELAYED_INCLUDE_MODE}.
     * Use this if you do not want to inline a resource.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     * @throws IllegalStateException If this method is called while not in {@link #DELAYED_INCLUDE_MODE}.
     */
    void requireResource(String resourceName);

    /**
     * Called by a component to indicate that a certain resource is required to be inserted into
     * this page.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer    The writer to write the links to if the {@link WebResourceManager.IncludeMode} equals {@link WebResourceManager#INLINE_INCLUDE_MODE}
     */
    public void requireResource(String resourceName, Writer writer);

    /**
     * Include the resources that have already been specified by the request in the page.  This is done by including
     * links to the resources that have been specified.
     * <p>
     * Example - if a 'javascript' resource has been specified, this method should output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     *
     * @param writer    The writer to write the links to
     */
    public void includeResources(Writer writer);

    /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     *
     * <pre><code>/s/{build num}/{system counter}/_</code></pre>
     *
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p>
     *
     * Typical usage is to replace:
     *
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix() %>/styles/global.css</code></pre>
     *
     * @return  A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix();

    /**
     * A helper method to return a prefix for 'system' static resources.  This method should be used for
     * resources that change more frequently than system resources, and therefore have their own resource counter.
     * <p>
     * Generally the implementation will return
     *
     * <pre><code>/s/{build num}/{system counter}/{resource counter}/_</code></pre>
     *
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p>
     *
     * Typical usage is to replace:
     *
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix(resourceCounter) %>/styles/global.css</code></pre>
     *
     * @param   resourceCounter    A number that represents the unique version of the resource you require.  Every time this
     * resource changes, you need to increment the resource counter
     * @return  A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix(String resourceCounter);

    /**
     * A helper method to return a prefix for 'plugin' resources.  Generally the implementation will return
     *
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     *
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p>
     *
     * Typical usage is to replace:
     *
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResourcePrefix(descriptor, resourceName) %></code></pre>
     *
     * @return  A url that can be used to request 'plugin' resources.
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName);

    /**
     * Whether resources should be included inline, or at the top of the page.  In most cases, you want to leave this
     * as the default.  However, for pages that don't have a decorator, you will not be able to 'delay' including
     * the resources (css, javascript), and therefore need to include them directly inline.
     *
     * @param includeMode   If there is no decorator for this request, set this to be {@link #INLINE_INCLUDE_MODE}
     * @see #DELAYED_INCLUDE_MODE
     * @see #INLINE_INCLUDE_MODE
     */
    public void setIncludeMode(IncludeMode includeMode);

    public static final IncludeMode DELAYED_INCLUDE_MODE = new IncludeMode()
    {
        public String getModeName()
        {
            return "delayed";
        }
    };

    public static final IncludeMode INLINE_INCLUDE_MODE = new IncludeMode()
    {
        public String getModeName()
        {
            return "inline";
        }
    };

    public static interface IncludeMode
    {
        public String getModeName();
    }

}
