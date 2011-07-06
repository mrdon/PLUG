package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Provides the prefixes a 'system' static resource as well as the base url of the application
 * to be used as a prefix for other web resources.
 * @since 2.10
 */
public interface WebResourceUrlProvider
{
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
     * @since 2.10
     */
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
     * @since 2.10
     */
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
     * If a module with the given key cannot be found null is returned.
     *
     * @param moduleCompleteKey complete plugin module key
     * @param resourceName      the name of the resource as defined in the plugin manifest
     * @param urlMode           specifies whether to use absolute URLs, relative URLs, or allow the concrete
     *                          implementation to decide
     * @return A url that can be used to request 'plugin' resources.
     * @since 2.3.0
     */
    String getStaticPluginResourceUrl(String moduleCompleteKey, String resourceName, UrlMode urlMode);

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
     * @see #getStaticPluginResourceUrl(String, String, UrlMode)
     * @since 2.3.0
     */
    String getStaticPluginResourceUrl(ModuleDescriptor<?> moduleDescriptor, String resourceName, UrlMode urlMode);

    /**
     * Constructs and returns url for the given resource.
     * This method is not responsible for adding any static caching url prefixes.
     *
     * @param pluginModuleKey a plugin module's complete key
     * @param resourceName the name of the resource described in the module
     */
    String getResourceUrl(String pluginModuleKey, String resourceName);

    /**
     * Returns the base URL for this application.  This method may return either an absolute or a relative URL.
     * Implementations are free to determine which mode to use based on any criteria of their choosing. For example, an
     * implementation may choose to return a relative URL if it detects that it is running in the context of an HTTP
     * request, and an absolute URL if it detects that it is not.  Or it may choose to always return an absolute URL, or
     * always return a relative URL.  Callers should only use this method when they are sure that either an absolute or
     * a relative URL will be appropriate, and should not rely on any particular observed behavior regarding how this
     * value is interpreted, which may vary across different implementations.
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link
     * #getBaseUrl(UrlMode)} with a {@code urlMode} value of {@link
     * UrlMode#AUTO}.
     *
     * @return the string value of the base URL of this application
     */
    String getBaseUrl();

    /**
     * Returns the base URL for this application in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may return either an absolute or a
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
     * @return the string value of the base URL of this application
     * @since 2.3.0
     */
    String getBaseUrl(UrlMode urlMode);
}
