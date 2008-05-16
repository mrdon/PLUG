package com.atlassian.plugin.webresource;

import java.util.Map;

/**
 * A formatter to format web resources into HTML.
 * <p/>
 * The {@link #matches(String)} method should be called before calling {@link #formatResource(String, String, Map)},
 * to ensure correct formatting of the resource.
 */
interface WebResourceFormatter
{
    /**
     * Returns a boolean indicating whether the WebResourceFormatter can support
     * formatting of the resource.
     *
     * @param name name of the resource
     * @return true if the formatter can format the resource, false otherwise.
     */
    boolean matches(String name);

    /**
     * Returns a formatted resource string.
     *
     * @param name       name of the resource
     * @param url        url path to the resource
     * @param parameters a {@link Map} of resource parameters
     * @return a formatted resource {@link String}.
     */
    String formatResource(String name, String url, Map parameters);

    /**
     * This can take a resource link and modify it so that it is in minified form.
     *
     * @param linkToResource the link to the resource
     * @return the minified link if applicable or the original resource link if minification does not apply
     */
    String minifyResourceLink(String linkToResource);
}
