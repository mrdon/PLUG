package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.RESOURCE_URL_PREFIX;

/**
 * This class complements the {@link com.atlassian.plugin.servlet.ResourceUrlParser} class.
 * It is responsible for formatting urls to plugin web resources. Use the ResourceUrlParser
 * to parse the formatted urls output by this class.
 */
public class ResourceUrlFormatter
{
    /**
     * Returns the formatted url to a single plugin resource.
     * <p/>
     * For example "urlPrefix/download/resources/moduleCompleteKey/resourceName"
     */
    public static String getResourceUrl(String urlPrefix, String moduleCompleteKey, String resourceName)
    {
        StringBuffer sb = new StringBuffer(urlPrefix);

        if(!urlPrefix.endsWith(PATH_SEPARATOR))
            sb.append(PATH_SEPARATOR);

        sb.append(SERVLET_PATH).append(PATH_SEPARATOR).append(RESOURCE_URL_PREFIX).append(PATH_SEPARATOR).
            append(moduleCompleteKey).append(PATH_SEPARATOR).append(resourceName);

        return sb.toString();
    }

    /**
     * Returns the formatted url to batch serve plugin resources of a particular type. The type is defined
     * by the given resourceExtension.
     * <p/>
     * For example "urlPrefix/download/resources/resourceExtention/moduleCompleteKey"
     */
    public static String getBatchResourceUrl(String urlPrefix, String moduleCompleteKey, String resourceExtention)
    {
        StringBuffer sb = new StringBuffer(urlPrefix);

        if(!urlPrefix.endsWith(PATH_SEPARATOR))
            sb.append(PATH_SEPARATOR);

        sb.append(SERVLET_PATH).append(PATH_SEPARATOR).append(RESOURCE_URL_PREFIX).append(PATH_SEPARATOR).
            append(resourceExtention).append(PATH_SEPARATOR).append(moduleCompleteKey);

        return sb.toString();
    }
}
