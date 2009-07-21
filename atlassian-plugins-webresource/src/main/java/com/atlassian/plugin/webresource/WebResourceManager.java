package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import java.io.Writer;

/**
 * Manage 'css', 'javascript' and other 'resources' that are usually linked at the top of pages using
 * <code>&lt;script&gt;</code> and <code>&lt;link&gt;</code> tags.
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
     * Writes out the resource tags to the previously required resources called via {@link #requireResource(String)}. If
     * you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource(), this method should
     * output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #includeResources(Writer,
     * UrlMode)} with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @param writer The writer to write the links to
     */
    public void includeResources(Writer writer);

    /**
     * Writes out the resource tags to the previously required resources called via {@link #requireResource(String)}. If
     * you need it as a String to embed the tags in a template, use {@link #getRequiredResources(UrlMode)}.
     * <p/>
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource(), this method should
     * output:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method uses absolute URLs, with URL scheme, hostname, port
     * (if non-standard for the scheme), and resource path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method uses relative URLs containing just the resource
     * path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may use either absolute or relative URLs. Implementations
     * are free to determine which mode to use based on any criteria of their choosing. For example, an implementation
     * may choose to use relative URLs if it detects that it is running in the context of an HTTP request, and absolute
     * URLs if it detects that it is not.  Or it may choose to always use absolute URLs, or always use relative URLs.
     * Callers should only use {@code UrlMode#AUTO} when they are sure that either absolute or relative URLs will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param writer  The writer to write the links to
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @since 2.3.0
     */
    public void includeResources(Writer writer, UrlMode urlMode);

    /**
     * Returns the resource tags for the previously required resources called via {@link #requireResource(String)}. If
     * you are outputting the value to a {@link Writer}, use {@link #includeResources(Writer)}.
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource(), this method should
     * return:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #getRequiredResources(UrlMode)}
     * with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @return the resource tags for all resources previously required
     * @see {@link #includeResources(Writer)}
     */
    public String getRequiredResources();

    /**
     * Returns the resource tags for the previously required resources called via {@link #requireResource(String)}. If
     * you are outputting the value to a {@link Writer}, use {@link #includeResources(Writer, UrlMode)}.
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource(), this method should
     * return:
     * <pre><code>
     *  &lt;script type=&quot;text/javascript&quot; src=&quot;$contextPath/scripts/javascript.js&quot;&gt;&lt;/script&gt;
     * </code></pre>
     * Similarly for other supported resources
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method uses absolute URLs, with URL scheme, hostname, port
     * (if non-standard for the scheme), and resource path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method uses relative URLs containing just the resource
     * path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may use either absolute or relative URLs. Implementations
     * are free to determine which mode to use based on any criteria of their choosing. For example, an implementation
     * may choose to use relative URLs if it detects that it is running in the context of an HTTP request, and absolute
     * URLs if it detects that it is not.  Or it may choose to always use absolute URLs, or always use relative URLs.
     * Callers should only use {@code UrlMode#AUTO} when they are sure that either absolute or relative URLs will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @return the resource tags for all resources previously required
     * @see {@link #includeResources(Writer, UrlMode)}
     * @since 2.3.0
     */
    public String getRequiredResources(UrlMode urlMode);

    /**
     * Writes the resource tags of the specified resource to the writer. If you need it as a String to embed the tags in
     * a template, use {@link #getResourceTags(String)}.
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #requireResource(String, Writer,
     * UrlMode)} with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer            The writer to write the resource tags to.
     */
    public void requireResource(String moduleCompleteKey, Writer writer);

    /**
     * Writes the resource tags of the specified resource to the writer. If you need it as a String to embed the tags in
     * a template, use {@link #getResourceTags(String, UrlMode)}.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method uses absolute URLs, with URL scheme, hostname, port
     * (if non-standard for the scheme), and resource path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method uses relative URLs containing just the resource
     * path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may use either absolute or relative URLs. Implementations
     * are free to determine which mode to use based on any criteria of their choosing. For example, an implementation
     * may choose to use relative URLs if it detects that it is running in the context of an HTTP request, and absolute
     * URLs if it detects that it is not.  Or it may choose to always use absolute URLs, or always use relative URLs.
     * Callers should only use {@code UrlMode#AUTO} when they are sure that either absolute or relative URLs will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer            The writer to write the resource tags to.
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @since 2.3.0
     */
    public void requireResource(String moduleCompleteKey, Writer writer, UrlMode urlMode);

    /**
     * Returns the resource tags of the specified resource. If you are outputting the value to a {@link Writer}, use
     * {@link #requireResource(String, java.io.Writer)}.
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #getResourceTags(String, UrlMode)}
     * with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @return the resource tags for the specified resource
     * @see {@link #requireResource(String, Writer)}
     * @since 2.2
     */
    public String getResourceTags(String moduleCompleteKey);

    /**
     * Returns the resource tags of the specified resource. If you are outputting the value to a {@link Writer}, use
     * {@link #requireResource(String, java.io.Writer, UrlMode)}.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method uses absolute URLs, with URL scheme, hostname, port
     * (if non-standard for the scheme), and resource path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method uses relative URLs containing just the resource
     * path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may use either absolute or relative URLs. Implementations
     * are free to determine which mode to use based on any criteria of their choosing. For example, an implementation
     * may choose to use relative URLs if it detects that it is running in the context of an HTTP request, and absolute
     * URLs if it detects that it is not.  Or it may choose to always use absolute URLs, or always use relative URLs.
     * Callers should only use {@code UrlMode#AUTO} when they are sure that either absolute or relative URLs will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @return the resource tags for the specified resource
     * @see {@link #requireResource(String, Writer, UrlMode)}
     * @since 2.3.0
     */
    public String getResourceTags(String moduleCompleteKey, UrlMode urlMode);

    /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix() %>/styles/global.css</code></pre>
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #getStaticResourcePrefix(UrlMode)}
     * with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @return A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix();

    /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix() %>/styles/global.css</code></pre>
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#AUTO}}, this method may return either an absolute or a
     * relative URL.  Implementations are free to determine which mode to use based on any criteria of their choosing.
     * For example, an implementation may choose to return a relative URL if it detects that it is running in the
     * context of an HTTP request, and an absolute URL if it detects that it is not.  Or it may choose to always return
     * an absolute URL, or always return a relative URL.  Callers should only use {@code
     * WebResourceManager.UrlMode#AUTO} when they are sure that either an absolute or a relative URL will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @return A prefix that can be used to prefix 'static system' resources.
     * @since 2.3.0
     */
    public String getStaticResourcePrefix(UrlMode urlMode);

    /**
     * A helper method to return a prefix for 'system' static resources.  This method should be used for resources that
     * change more frequently than system resources, and therefore have their own resource counter.
     * <p/>
     * Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{resource counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix(resourceCounter) %>/styles/global.css</code></pre>
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #getStaticResourcePrefix(String,
     * UrlMode)} with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @param resourceCounter A number that represents the unique version of the resource you require.  Every time this
     *                        resource changes, you need to increment the resource counter
     * @return A prefix that can be used to prefix 'static system' resources.
     */
    public String getStaticResourcePrefix(String resourceCounter);

    /**
     * A helper method to return a prefix for 'system' static resources.  This method should be used for resources that
     * change more frequently than system resources, and therefore have their own resource counter.
     * <p/>
     * Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{resource counter}/_</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/styles/global.css</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticResourcePrefix(resourceCounter) %>/styles/global.css</code></pre>
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#AUTO}}, this method may return either an absolute or a
     * relative URL.  Implementations are free to determine which mode to use based on any criteria of their choosing.
     * For example, an implementation may choose to return a relative URL if it detects that it is running in the
     * context of an HTTP request, and an absolute URL if it detects that it is not.  Or it may choose to always return
     * an absolute URL, or always return a relative URL.  Callers should only use {@code
     * WebResourceManager.UrlMode#AUTO} when they are sure that either an absolute or a relative URL will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param resourceCounter A number that represents the unique version of the resource you require.  Every time this
     *                        resource changes, you need to increment the resource counter
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @return A prefix that can be used to prefix 'static system' resources.
     * @since 2.3.0
     */
    public String getStaticResourcePrefix(String resourceCounter, UrlMode urlMode);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResource(descriptor, resourceName) %></code></pre>
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link #getStaticPluginResource(String,
     * String, UrlMode)} with a {@code urlMode} value of {@link WebResourceManager.UrlMode#AUTO}.
     *
     * @param moduleCompleteKey complete plugin module key
     * @param resourceName      the name of the resource as defined in the plugin manifest
     * @return A url that can be used to request 'plugin' resources.
     */
    public String getStaticPluginResource(String moduleCompleteKey, String resourceName);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResource(descriptor, resourceName) %></code></pre>
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#AUTO}}, this method may return either an absolute or a
     * relative URL.  Implementations are free to determine which mode to use based on any criteria of their choosing.
     * For example, an implementation may choose to return a relative URL if it detects that it is running in the
     * context of an HTTP request, and an absolute URL if it detects that it is not.  Or it may choose to always return
     * an absolute URL, or always return a relative URL.  Callers should only use {@code
     * WebResourceManager.UrlMode#AUTO} when they are sure that either an absolute or a relative URL will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param moduleCompleteKey complete plugin module key
     * @param resourceName      the name of the resource as defined in the plugin manifest
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @return A url that can be used to request 'plugin' resources.
     * @since 2.3.0
     */
    public String getStaticPluginResource(String moduleCompleteKey, String resourceName, UrlMode urlMode);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResource(descriptor, resourceName) %></code></pre>
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link
     * #getStaticPluginResource(ModuleDescriptor, String, UrlMode)} with a {@code urlMode} value of {@link
     * WebResourceManager.UrlMode#AUTO}.
     *
     * @param moduleDescriptor plugin module descriptor that contains the resource
     * @param resourceName     the name of the resource as defined in the plugin manifest
     * @return returns the url of this plugin resource
     * @see {@link #getStaticPluginResource(String, String)}
     */
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * <pre><code>/s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name</code></pre>
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * <pre><code>&lt;%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name</code></pre>
     * with
     * <pre><code>&lt;%= webResourceManager.getStaticPluginResource(descriptor, resourceName) %></code></pre>
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link WebResourceManager.UrlMode#AUTO}}, this method may return either an absolute or a
     * relative URL.  Implementations are free to determine which mode to use based on any criteria of their choosing.
     * For example, an implementation may choose to return a relative URL if it detects that it is running in the
     * context of an HTTP request, and an absolute URL if it detects that it is not.  Or it may choose to always return
     * an absolute URL, or always return a relative URL.  Callers should only use {@code
     * WebResourceManager.UrlMode#AUTO} when they are sure that either an absolute or a relative URL will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param moduleDescriptor plugin module descriptor that contains the resource
     * @param resourceName     the name of the resource as defined in the plugin manifest
     * @param urlMode          specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                         implementation to decide
     * @return returns the url of this plugin resource
     * @see {@link #getStaticPluginResource(String, String, UrlMode)}
     * @since 2.3.0
     */
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName, UrlMode urlMode);

    /**
     * A formatting mode for URLs. Used to specify to {@code WebResourceManager} methods whether to use absolute URLs,
     * relative URLs, or allow the concrete implementation to decide
     */
    enum UrlMode
    {
        /** Absolute URL format, with URL scheme, hostname, port (if non-standard for the scheme), and context path. */
        ABSOLUTE,
        /** Relative URL format, containing just the context path. */
        RELATIVE,
        /** Unspecified URL format, indicating that either absolute or relative URLs are acceptable */
        AUTO
    }


    // Deprecated methods

    /**
     * @deprecated Use #getStaticPluginResource instead
     */
    @Deprecated
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
    @Deprecated
    public void setIncludeMode(IncludeMode includeMode);

    /**
     * @deprecated Since 2.2. Use {@link #writeResourceTags(String, Writer)} instead.
     */
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    public static interface IncludeMode
    {
        public String getModeName();
    }

}
