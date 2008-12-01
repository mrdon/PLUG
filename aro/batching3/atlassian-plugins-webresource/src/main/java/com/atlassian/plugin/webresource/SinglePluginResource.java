package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.RESOURCE_URL_PREFIX;

import java.util.Map;
import java.util.Collections;

/**
 * Represents a single plugin resource.
 *
 * It provides methods to parse and generate urls to locate a single plugin resource.
 */
public class SinglePluginResource implements PluginResource
{
    /**
     * The url prefix to a single plugin resource: "/download/resources/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + RESOURCE_URL_PREFIX;

    private String resourceName;
    private String moduleCompleteKey;
    private final boolean cached;

    public SinglePluginResource(String resourceName, String moduleCompleteKey, boolean cached)
    {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.cached = cached;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public Map<String, String> getParams()
    {
        return Collections.EMPTY_MAP;
    }

    public boolean isCacheSupported()
    {
        return cached;
    }

    /**
     * Returns a url string in the format: /download/resources/MODULE_COMPLETE_KEY/RESOURCE_NAME
     *
     * e.g. /download/resources/example.plugin:webresources/foo.css
     */
    public String getUrl()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(moduleCompleteKey).append(PATH_SEPARATOR)
            .append(resourceName);

        return sb.toString();

    }

    public static boolean matches(String url)
    {
        return url.indexOf(URL_PREFIX) != -1;
    }

    /**
     * Parses the given url and query parameter map into a SinglePluginResource. Query paramters must be
     * passed in through the map, any in the url String will be ignored.
     * @param url the url to parse
     * @param queryParams a map of String key and value pairs representing the query parameters in the url
     */
    public static SinglePluginResource parse(String url)
    {
        int indexOfPrefix = url.indexOf(SinglePluginResource.URL_PREFIX);
        String libraryAndResource = url.substring(indexOfPrefix + SinglePluginResource.URL_PREFIX.length() + 1);

        if(libraryAndResource.indexOf('?') != -1) // remove query parameters
        {
            libraryAndResource = libraryAndResource.substring(0, libraryAndResource.indexOf('?'));
        }

        String[] parts = libraryAndResource.split("/", 2);

        if (parts.length != 2)
            return null;

        return new SinglePluginResource(parts[1], parts[0], url.substring(0, indexOfPrefix).length() > 0);
    }
}
