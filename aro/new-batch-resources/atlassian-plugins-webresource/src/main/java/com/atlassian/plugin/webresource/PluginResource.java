package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;

/**
 * Identifies a resource in the plugin system. Normally generated from a request URL.
 */
public class PluginResource
{
    public static final String URL_PREFIX = PATH_SEPARATOR + AbstractFileServerServlet.SERVLET_PATH + PATH_SEPARATOR + AbstractFileServerServlet.RESOURCE_URL_PREFIX;
    
    private final String moduleCompleteKey;
    private final String resourceName;

    /**
     * @param moduleCompleteKey the key of the plugin module where the resource can be found, or the key
     * of the plugin if the resource is specified at the plugin level.
     * @param resourceName the name of the resource.
     */
    public PluginResource(String moduleCompleteKey, String resourceName)
    {
        this.moduleCompleteKey = moduleCompleteKey;
        this.resourceName = resourceName;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getUrl()
    {
        // e.g. /download/resources/example.plugin:webresources/foo.css
        StringBuffer sb = new StringBuffer();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(moduleCompleteKey).append(PATH_SEPARATOR)
            .append(resourceName);

        return sb.toString();
    }

    public static PluginResource parse(String resourceUrl)
    {
        int indexOfPrefix = resourceUrl.indexOf(URL_PREFIX);
        String libraryAndResource = resourceUrl.substring(indexOfPrefix + URL_PREFIX.length() + 1);
        String[] parts = libraryAndResource.split("/", 2);

        if (parts.length != 2)
            return null;

        return new PluginResource(parts[0], parts[1]);
    }

    public String toString()
    {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", resourceName=" + resourceName + "]";
    }
}
