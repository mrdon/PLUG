package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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

    /**
     * A constructor that creates a default resource name for the batch in the format: moduleCompleteKey.type
     * For example: test.plugin:resources.js
     * <p/>
     * Note that name of the batch does not identify what the batch includes and could have been static e.g. batch.js
     */
    public BatchPluginResource(String moduleCompleteKey, String type, Map<String, String> params)
    {
        this(moduleCompleteKey + "." + type, moduleCompleteKey, type, params);
    }

    /**
     * This constructor should only ever be used internally within this class. It does not ensure that the resourceName's
     * file extension is the same as the given type. It is up to the calling code to ensure this.
     */
    private BatchPluginResource(String resourceName, String moduleCompleteKey, String type, Map<String, String> params)
    {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.type = type;
        this.params = params;
        this.resources = new ArrayList<DownloadableResource>();
    }

    /**
     * @return true if there are no resources included in this batch
     */
    public boolean isEmpty()
    {
        return resources.isEmpty();
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
        log.debug("Start to serve batch " + toString());
        for (DownloadableResource resource : resources)
        {
            resource.serveResource(request, response);
        }
    }
    
    public void streamResource(OutputStream out)
    {
        for (DownloadableResource resource : resources)
        {
            resource.streamResource(out);
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
        String[] parts = typeAndModuleKey.split("/", 2);

        if (parts.length < 2)
            return null;

        String moduleKey = parts[0];
        String resourceName = parts[1];
        String type = resourceName.substring(resourceName.lastIndexOf('.') + 1);

        return new BatchPluginResource(resourceName, moduleKey, type, queryParams);
    }

    public static boolean matches(String url)
    {
        return url.indexOf(URL_PREFIX) != -1;
    }

    /**
     * Returns a url string in the format: /download/batch/MODULE_COMPLETE_KEY/resourceName?PARAMS
     *
     * e.g. /download/batch/example.plugin:webresources/example.plugin:webresources.css?ie=true
     * <p/>
     * It is important for the url structure to be:
     * 1. the same number of sectioned paths as the SinglePluginResource
     * 2. include the module completey key in the path before the resource name
     * This is due to css resources referencing other resources such as images in relative path forms.
     */
    public String getUrl()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(moduleCompleteKey).append(PATH_SEPARATOR)
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

    public String getResourceName()
    {
        return resourceName;
    }

    public Map<String, String> getParams()
    {
        return Collections.unmodifiableMap(params);
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
