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
     * this page.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     */
    public void requireResource(String resourceName);

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
     * @param contextPath   For linking to resources, we need to specify the context path
     * @param writer    The writer to write the links to
     * @throws java.io.IOException  If the {@link Writer} passed in throws an exception when being written to
     */
    public void includeResources(String contextPath, Writer writer) throws IOException;

    /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     *
     * <pre><code>/s/{build num}/{system date}/c</code></pre>
     *
     * Note that the servlet context is not prepended, and there is no trailing slash.
     * <p>
     *
     * Typical usage is to replace:
     *
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= request.getContextPath() + webResourceManager.getStaticResourcePrefix() %>/styles/global.css</code></pre>
     *
     * @return  A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix();

    /**
     * A helper method to return a prefix for 'plugin' static resources.  Generally the implementation will return
     *
     * <pre><code>/s/{build num}/{system date}/{plugin version}/c/</code></pre>
     *
     * Note that the servlet context is not prepended, and there is no trailing slash.
     * <p>
     *
     * Typical usage is to replace:
     *
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= request.getContextPath() + webResourceManager.getStaticPluginResourcePrefix(descriptor, resourceDescriptor) %>/styles/global.css</code></pre>
     *
     * @return  A prefix that can be used to prefix 'static plugin' resources.
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor);

}
