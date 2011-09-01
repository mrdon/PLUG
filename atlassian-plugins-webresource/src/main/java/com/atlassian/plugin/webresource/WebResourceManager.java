package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

import com.google.common.base.Supplier;

import java.io.Writer;

/**
 * Manage 'css', 'javascript' and other 'resources' that are usually linked at the top of pages using {@code <script>
 * and <link>} tags.
 * <p/>
 * By using the WebResourceManager, components can declare dependencies on javascript and css that they would otherwise
 * have to embed inline (which means that it can't be cached, and are often duplicated in a page).
 */
public interface WebResourceManager
{
    /**
     * Indicates that a given plugin web resource is required. All resources called via this method must be included
     * when {@link #includeResources(Writer)} is called.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @see #includeResources(Writer, UrlMode)
     */
    void requireResource(String moduleCompleteKey);

    /**
     * Writes out the resource tags for a specified set of required resources and their dependencies. Does not write out
     * tags for resources specified in calls to {@link #requireResource(String)}.
     *
     * @param moduleCompleteKeys The set of web resource modules to include
     * @param writer the writer to write the links to
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @since 2.4.0
     */
    void includeResources(Iterable<String> moduleCompleteKeys, Writer writer, UrlMode urlMode);

    /**
     * This is the equivalent of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * the given url mode and a default web resource filter that is dependent on the implementation.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     * @since 2.3.0
     */
    void includeResources(Writer writer, UrlMode urlMode);

    /**
     * Writes out the resource tags to the previously required resources called via {@link #requireResource(String)} for
     * the specified resource type. If you need it as a String to embed the tags in a template, use
     * {@link #getRequiredResources(UrlMode)}.
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource() and this method is called
     *  with {@link JavascriptWebResource.FILTER_INSTANCE}, it should output:
     * <pre>
     *  {@literal <script type="text/javascript" src="$contextPath/scripts/javascript.js"></script>}
     * </pre>
     * Similarly for other supported resources.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.  See {@link UrlMode} for details of the different options for URL format.
     *
     * @param writer  the writer to write the links to
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @param webResourceFilter the web resource filter to filter resources on
     * @since 2.4
     */
    void includeResources(Writer writer, UrlMode urlMode, WebResourceFilter webResourceFilter);

    /**
     * This is the equivalent of calling {@link #getRequiredResources(UrlMode, WebResourceFilter)} with the given url
     * mode and a default filter that is dependent on the implementation.
     *
     * @return the resource tags for all resources previously required
     * @see #includeResources(Writer, UrlMode)
     * @see #getRequiredResources(UrlMode, WebResourceFilter)
     * @since 2.3.1
     */
    String getRequiredResources(UrlMode urlMode);

    /**
     * Returns the resource tags for the previously required resources called via {@link #requireResource(String)} that
     * match the specified web resource filter. If you are outputting the value to a {@link Writer}, use
     * {@link #includeResources(Writer, UrlMode)}.
     * <p/>
     * Example - if a 'javascript' resource has been required earlier with requireResource() and this method is called
     *  with {@link JavascriptWebResource.FILTER_INSTANCE}, it should return:
     * <pre>
     *  {@literal <script type="text/javascript" src="$contextPath/scripts/javascript.js"></script>}
     * </pre>
     * Similarly for other supported resources.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.  See {@link UrlMode} for details of the different options for URL format.
     * <p/>
     *
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @param webResourceFilter the web resource filter to filter resources on
     * @return the resource tags for all resources previously required
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     * @since 2.4
     */
    String getRequiredResources(UrlMode urlMode, WebResourceFilter webResourceFilter);

    /**
     * Writes the resource tags of the specified resource to the writer. If you need it as a String to embed the tags in
     * a template, use {@link #getResourceTags(String, UrlMode)}.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.  See {@link UrlMode} for details of the different options for URL format.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param writer            The writer to write the resource tags to.
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @since 2.3.0
     */
    void requireResource(String moduleCompleteKey, Writer writer, UrlMode urlMode);

    /**
     * Writes the resource tags of all resources that have the given context specified in their descriptor.
     *
     * @param context The name of the context for which you want to require resources (eg "atl.admin")
     * @since 2.5.0
     */
    void requireResourcesForContext(String context);

    /**
     * Returns the resource tags of the specified resource. If you are outputting the value to a {@link Writer}, use
     * {@link #requireResource(String, java.io.Writer, UrlMode)}.
     * <p/>
     * This method formats resource URLs in either relative or absolute format, depending on the value of {@code
     * urlMode}.  See {@link UrlMode} for details of the different options for URL format.
     *
     * @param moduleCompleteKey The fully qualified plugin web resource module (eg <code>jira.webresources:scriptaculous</code>)
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @return the resource tags for the specified resource
     * @see #requireResource(String, Writer, UrlMode)
     * @since 2.3.0
     */
    String getResourceTags(String moduleCompleteKey, UrlMode urlMode);

    /**
     * Executes a Supplier within a new WebResourceManager context. The request-local state manipulated by requireResource
     * and requireResourcesForContext is preserved, an empty state is initialized for the execution of nestedExecution and
     * after the nestedExecution is run, the old state is restored.
     *
     * If no return is required from the nestedExecution then Suppier<Void> can be used.
     *
     * Useful for rendering of pages which include nested pages (such as gadgets), which need to resolve the requirements
     * of the inner pages without polluting the outer page's resources.
     *
     * @param nestedExecution the code to be executed in the empty context.
     * @return the value returned by nestedExection.get()
     * @since 2.10.0
     */
    <T> T executeInNewContext(Supplier<T> nestedExecution);

    // Deprecated methods

   /**
     * A helper method to return a prefix for 'system' static resources.  Generally the implementation will return
     * <p/>
     * {@code /s/{build num}/{system counter}/_}
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * {@code <%= request.getContextPath() %>/styles/global.css} with {@code <%= webResourceManager.getStaticResourcePrefix()
     * %>/styles/global.css}
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * See {@link UrlMode} for details of the different options for URL format.
     *
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @return A prefix that can be used to prefix 'static system' resources.
     * @since 2.3.0
     * @deprecated As of 2.9.0, replaced by {@link WebResourceUrlProvider#getStaticResourcePrefix(UrlMode)}
     */
    @Deprecated
    String getStaticResourcePrefix(UrlMode urlMode);

    /**
     * A helper method to return a prefix for 'system' static resources.  This method should be used for resources that
     * change more frequently than system resources, and therefore have their own resource counter.
     * <p/>
     * Generally the implementation will return
     * <p/>
     * {@code /s/{build num}/{system counter}/{resource counter}/_}
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * {@code <%= request.getContextPath() %>/styles/global.css} with {@code <%= webResourceManager.getStaticResourcePrefix(resourceCounter)
     * %>/styles/global.css}
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * See {@link UrlMode} for details of the different options for URL format.
     *
     * @param resourceCounter A number that represents the unique version of the resource you require.  Every time this
     *                        resource changes, you need to increment the resource counter
     * @param urlMode         specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                        implementation to decide
     * @return A prefix that can be used to prefix 'static system' resources.
     * @since 2.3.0
     * @deprecated As of 2.9.0, replaced by {@link WebResourceUrlProvider#getStaticResourcePrefix(String, UrlMode)}
     */
    @Deprecated
    String getStaticResourcePrefix(String resourceCounter, UrlMode urlMode);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * {@code /s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name}
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * {@code <%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name} with {@code <%=
     * webResourceManager.getStaticPluginResource(descriptor, resourceName) %>}
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * See {@link UrlMode} for details of the different options for URL format.
     *
     * @param moduleCompleteKey complete plugin module key
     * @param resourceName      the name of the resource as defined in the plugin manifest
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @return A url that can be used to request 'plugin' resources.
     * @since 2.3.0
     * @deprecated As of 2.9.0, replaced by {@link WebResourceUrlProvider#getStaticPluginResourceUrl(String, String, UrlMode)}
     */
    @Deprecated
    String getStaticPluginResource(String moduleCompleteKey, String resourceName, UrlMode urlMode);

    /**
     * A helper method to return a url for 'plugin' resources.  Generally the implementation will return
     * <p/>
     * {@code /s/{build num}/{system counter}/{plugin version}/_/download/resources/plugin.key:module.key/resource.name}
     * <p/>
     * Note that the servlet context is prepended, and there is no trailing slash.
     * <p/>
     * Typical usage is to replace:
     * <p/>
     * {@code <%= request.getContextPath() %>/download/resources/plugin.key:module.key/resource.name} with {@code <%=
     * webResourceManager.getStaticPluginResource(descriptor, resourceName) %>}
     * <p/>
     * This method returns a URL in either a relative or an absolute format, depending on the value of {@code urlMode}.
     * See {@link UrlMode} for details of the different options for URL format.
     *
     * @param moduleDescriptor plugin module descriptor that contains the resource
     * @param resourceName     the name of the resource as defined in the plugin manifest
     * @param urlMode          specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                         implementation to decide
     * @return returns the url of this plugin resource
     * @see #getStaticPluginResource(String, String, UrlMode)
     * @since 2.3.0
     * @deprecated As of 2.9.0, replaced by {@link WebResourceUrlProvider#getStaticPluginResourceUrl(com.atlassian.plugin.ModuleDescriptor, String, UrlMode)}
     */
    @Deprecated
    String getStaticPluginResource(ModuleDescriptor<?> moduleDescriptor, String resourceName, UrlMode urlMode);

    //
    // deprecated since 2.3 or earlier
    //

    /**
    * @deprecated As of 2.3.0, replaced by {@link #includeResources(Writer, UrlMode)}
    */
    @Deprecated
    void includeResources(Writer writer);

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getRequiredResources(UrlMode)}
     */
    @Deprecated
    String getRequiredResources();

    /**
     * @deprecated As of 2.3.0, replaced by {@link #requireResource(String, Writer, UrlMode)}
     */
    @Deprecated
    void requireResource(String moduleCompleteKey, Writer writer);

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getResourceTags(String, UrlMode)}
     */
    @Deprecated
    String getResourceTags(String moduleCompleteKey);

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getStaticResourcePrefix(UrlMode)}
     */
    @Deprecated
    String getStaticResourcePrefix();

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getStaticResourcePrefix(String, UrlMode)}
     */
    @Deprecated
    String getStaticResourcePrefix(String resourceCounter);

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getStaticPluginResource(String, String, UrlMode)}
     */
    @Deprecated
    String getStaticPluginResource(String moduleCompleteKey, String resourceName);

    /**
     * @deprecated As of 2.3.0, replaced by {@link #getStaticPluginResource(ModuleDescriptor, String, UrlMode)}
     */
    @Deprecated
    String getStaticPluginResource(ModuleDescriptor<?> moduleDescriptor, String resourceName);

    /**
     * @deprecated Since 2.2 Use #getStaticPluginResource instead
     */
    @Deprecated
    String getStaticPluginResourcePrefix(ModuleDescriptor<?> moduleDescriptor, String resourceName);

    /**
     * @see #INLINE_INCLUDE_MODE
     * @deprecated Since 2.2.
     */
    @Deprecated
    void setIncludeMode(IncludeMode includeMode);

    /**
     * @deprecated Since 2.2. Use {@link #requireResource(String, Writer, UrlMode)} instead.
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
