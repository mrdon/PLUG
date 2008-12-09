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
     * @see {@link #writeRequiredResources(Writer)}
     */
    public String getRequiredResources();

    /**
     * Writes the resource tags of the specified resource to the writer.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     * @since 2.2
     */
    public void writeResourceTags(String moduleCompleteKey, Writer writer);

    /**
     * @see {@link #writeResourceTags(String, Writer)}
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
     * @return returns the url of this plugin resource
     */
    public String getStaticPluginResource(String moduleCompleteKey, String resourceName);


    // Deprecated methods

    /**
     * @deprecated Since 2.2. Use {@link #getStaticPluginResource(String, String)} instead.
     */
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName);

    /**
     * Called by a component to indicate that a certain resource is required to be inserted into
     * this page.
     *
     * @param resourceName The fully qualified plugin name to include (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer       The writer to write the links to if the {@link WebResourceManager.IncludeMode} equals {@link WebResourceManager#INLINE_INCLUDE_MODE}
     * @deprecated Since 2.2. Use #writeResourceTags instead.
     */
    public void requireResource(String resourceName, Writer writer);

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
