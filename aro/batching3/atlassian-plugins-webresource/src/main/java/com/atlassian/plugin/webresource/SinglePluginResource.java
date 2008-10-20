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
    private final String staticUrlPrefix;

    public SinglePluginResource(String resourceName, String moduleCompleteKey, String staticUrlPrefix)
    {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.staticUrlPrefix = staticUrlPrefix;
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

    public String getUrl()
    {
        // "/download/resources/moduleCompleteKey:resourceName"
        StringBuffer sb = new StringBuffer(staticUrlPrefix);
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(moduleCompleteKey).append(PATH_SEPARATOR)
            .append(resourceName);

        return sb.toString();

    }

    public static boolean matches(String url)
    {
        return url.indexOf(URL_PREFIX) != -1;
    }

    public static SinglePluginResource parse(String url)
    {
        int indexOfPrefix = url.indexOf(SinglePluginResource.URL_PREFIX);
        String libraryAndResource = url.substring(indexOfPrefix + SinglePluginResource.URL_PREFIX.length() + 1);
        String[] parts = libraryAndResource.split("/", 2);

        if (parts.length != 2)
            return null;

        return new SinglePluginResource(parts[1], parts[0], url.substring(0, indexOfPrefix));
    }
}