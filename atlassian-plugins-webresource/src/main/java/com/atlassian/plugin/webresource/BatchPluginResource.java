package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a batch of plugin resources. <p/>
 *
 * It provides methods to parse and generate urls to locate a batch of plugin resources. <p/>
 *
 * Note BatchPluginResource is also a type of {@link DownloadableResource}. The underlying implementation simply
 * keeps a list of {@link DownloadableResource} of which this batch represents and delegates method calls.
 * @since 2.2
 */
public class BatchPluginResource implements DownloadableResource, PluginResource
{
    private static final Log log = LogFactory.getLog(BatchPluginResource.class);

    /**
     * The url prefix for a batch of plugin resources: "/download/batch/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "batch";

    final private String type;
    final private String moduleCompleteKey;
    final private Map<String, String> params;
    final private String resourceName;
    final private List<DownloadableResource> resources;

    public BatchPluginResource(String moduleCompleteKey, String type, Map<String, String> params)
    {
        this.moduleCompleteKey = moduleCompleteKey;
        this.type = type;
        this.params = params;
        this.resourceName = moduleCompleteKey + "." + type;
        this.resources = new ArrayList<DownloadableResource>();
    }

    public void add(DownloadableResource resource)
    {
        resources.add(resource);
    }

    public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response)
    {
        for (DownloadableResource resource : resources)
        {
            if (resource.isResourceModified(request, response))
                return true;
        }
        return false;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        log.info("Start to serve batch " + toString());
        for (DownloadableResource resource : resources)
        {
            resource.serveResource(request, response);
        }
    }

    public String getContentType()
    {
        String contentType = params.get("content-type");
        if (contentType != null)
            return contentType;
        
        return null;
    }

    /**
     * Parses the given url and query parameter map into a BatchPluginResource. Query paramters must be
     * passed in through the map, any in the url String will be ignored.
     * @param url the url to parse
     * @param queryParams a map of String key and value pairs representing the query parameters in the url
     */
    public static BatchPluginResource parse(String url, Map<String, String> queryParams)
    {
        int startIndex = url.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;

        if (url.indexOf('?') != -1) // remove query parameters
        {
            url = url.substring(0, url.indexOf('?'));
        }

        String typeAndModuleKey = url.substring(startIndex);
        String[] parts = typeAndModuleKey.split("/");

        if (parts.length < 2)
            return null;

        String type = parts[0];
        String moduleKey = parts[1];
        if (moduleKey.endsWith("." + type))
        {
            moduleKey = moduleKey.substring(0, moduleKey.lastIndexOf("." + type));
        }

        return new BatchPluginResource(moduleKey, type, queryParams);
    }

    public static boolean matches(String url)
    {
        return url.indexOf(URL_PREFIX) != -1;
    }

    /**
     * Returns a url string in the format: /download/batch/TYPE/MODULE_COMPLETE_KEY.TYPE?PARAMS
     *
     * e.g. /download/batch/css/example.plugin:webresources.css?ie=true
     */
    public String getUrl()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(type).append(PATH_SEPARATOR)
            .append(resourceName);

        if(params.size() > 0 )
        {
            sb.append("?");
            int count = 0;

            for (Map.Entry<String, String> entry: params.entrySet())
            {
                sb.append(entry.getKey()).append("=").append(entry.getValue());

                if(++count < params.size())
                    sb.append("&");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the resource name in the format moduleCompleteKey.type
     * For example: test.plugin:resources.js
     */
    public String getResourceName()
    {
        return resourceName;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public boolean isCacheSupported()
    {
        return !"false".equals(params.get("cache"));
    }

    public String getType()
    {
        return type;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchPluginResource that = (BatchPluginResource) o;

        if (moduleCompleteKey != null ? !moduleCompleteKey.equals(that.moduleCompleteKey) : that.moduleCompleteKey != null)
            return false;
        if (params != null ? !params.equals(that.params) : that.params != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (moduleCompleteKey != null ? moduleCompleteKey.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", type=" + type + ", params=" + params + "]";
    }
}
