package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import java.io.Writer;

/**
 * Manage 'css', 'javascript' and other 'resources' that are usually linked at the top of pages using
 * <code>&lt;script&gt;</code> and <code>&lt;link&gt; tags.
 * <p/>
 * By using the WebResourceManager, components can declare dependencies on javascript and css that they would
 * otherwise have to embed inline (which means that it can't be cached, and are often duplicated in a page).
 */
public interface WebResourceManager
{
    /**
     * Indicates to that a given plugin web resource is required. All resources called via this method must be
     * included when {@link #includeResources(Writer)} is called.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @see #includeResources(Writer)
     */
    public void requireResource(String moduleCompleteKey);

    /**
     * Writes out the resource tags to the previously required resources called via {@link #requireResource(String)}.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     *
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource(), this method should output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     *
     * @param writer The writer to write the links to
     */
    public void includeResources(Writer writer);

    /**
     * Writes out the resource tags of the provided type to the previously required resources called via {@link #requireResource(String)}.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources(String)}.
     *
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource() and the passed type
     * parameter is "js" this method should output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     *
     * @param writer The writer to write the links to
     * @param type The type of resource to create links for
     */
    public void includeResources(Writer writer, String type);

    /**
     * @see {@link #includeResources(Writer)}
     */
    public String getRequiredResources();

    /**
     * @see {@link #includeResources(Writer, String)}
     */
    public String getRequiredResources(String type);

    /**
     * Writes the resource tags of the specified resource to the writer.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer       The writer to write the resource tags to.
     */
    public void requireResource(String moduleCompleteKey, Writer writer);

    /**
     * @see {@link #requireResource(String, Writer)}
     * @since 2.2
     */
    public String getResourceTags(String moduleCompleteKey);

    /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix() %>/styles/global.css</code></pre>
     *
     * @return A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix();

    /**
     * A helper method to return a prefix for 'system' static resources.  This method should be used for
     * resources that change more frequently than system resources, and therefore have their own resource counter.
     * <p/>
     * Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{resource counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix(resourceCounter) %>/styles/global.css</code></pre>
     *
     * @param resourceCounter A number that represents the unique version of the resource you require.  Every time this
     *                        resource changes, you need to increment the resource counter
     * @return A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix(String resourceCounter);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResource(descriptor, resourceName) %></code></pre>
     *
     * @param moduleCompleteKey complete plugin module key
     * @return A url that can be used to request 'plugin' resources.
     */
    public String getStaticPluginResource(String moduleCompleteKey, String resourceName);


    /**
     * @see {@link #getStaticPluginResource(String, String)}
     * @param moduleDescriptor plugin module containing the required resource
     * @param resourceName resource in the module to return
     * @return returns the url of this plugin resource
     */
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName);


    // Deprecated methods

    /**
     * @deprecated Use #getStaticPluginResource instead
     */
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName);

    /**
     * Whether resources should be included inline, or at the top of the page.  In most cases, you want to leave this
     * as the default.  However, for pages that don't have a decorator, you will not be able to 'delay' including
     * the resources (css, javascript), and therefore need to include them directly inline.
     *
     * @param includeMode If there is no decorator for this request, set this to be {@link #INLINE_INCLUDE_MODE}
     * @see #DELAYED_INCLUDE_MODE
     * @see #INLINE_INCLUDE_MODE
     * @deprecated Since 2.2.
     */
    public void setIncludeMode(IncludeMode includeMode);

    /**
     * @deprecated Since 2.2. Use {@link #writeResourceTags(String, Writer)} instead.
     */
    public static final IncludeMode DELAYED_INCLUDE_MODE = new IncludeMode()
    {
        public String getModeName()
        {
            return "delayed";
        }
    };

    /**
     * @deprecated Since 2.2. Use {@link #requireResource(String)}  instead.
     */
    public static final IncludeMode INLINE_INCLUDE_MODE = new IncludeMode()
    {
        public String getModeName()
        {
            return "inline";
        }
    };

    /**
     * @deprecated Since 2.2
     */
    public static interface IncludeMode
    {
        public String getModeName();
    }

}
