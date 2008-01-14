package com.atlassian.plugin.webresource;

import java.util.Map;

public interface WebResourceFormatter
{
    /**
     * Returns a boolean indicating whether the WebResourceFormatter can support
     * formatting of the resource.
     * @param name name of the resource
     * @return true if the formatter can format the resource, false otherwise.
     */
    boolean matches(String name);

    /**
     * Returns a formatted resource string.
     * @param name name of the resource
     * @param url url path to the resource
     * @param parameters a {@link Map} of resource parameters
     * @return a formatted resource {@link String}.
     */
    String formatResource(String name, String url, Map parameters);
}
