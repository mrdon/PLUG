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
 */
public class BatchPluginResource implements DownloadableResource, PluginResource
{
    private static final Log log = LogFactory.getLog(BatchPluginResource.class);

    /**
     * The url prefix for a batch of plugin resources: "/download/batch/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "batch";

    private String resourceName;
    private String type;
    private String moduleCompleteKey;
    private Map<String, String> params;
    private String staticUrlPrefix;
    private List<DownloadableResource> resources;

    public BatchPluginResource(String batchName, String moduleCompleteKey, String type, Map<String, String> params, String staticUrlPrefix)
    {
        this.moduleCompleteKey = moduleCompleteKey;
        this.type = type;
        this.params = params;
        this.staticUrlPrefix = staticUrlPrefix;
        this.resourceName = batchName + "." + type;
        this.resources = new ArrayList<DownloadableResource>();
    }

    public void add(DownloadableResource resource)
    {
        resources.add(resource);
    }

    public boolean checkResourceNotModified(HttpServletRequest request, HttpServletResponse response)
    {
        for(DownloadableResource resource : resources)
        {
            if(resource.checkResourceNotModified(request, response))
                return true;
        }
        return false;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        log.info("Start to serve batch " + toString());
        for(DownloadableResource resource : resources)
        {
            resource.serveResource(request, response);
        }
    }

    public String getContentType()
    {
        //todo when content type specified in params
        return null;
    }

    public static BatchPluginResource parse(String url, Map<String, String> queryParams)
    {
        int startIndex = url.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;

        String typeAndModuleKey = url.substring(startIndex);
        String[] parts = typeAndModuleKey.split("/");

        if (parts.length < 2)
            return null;

        String type = parts[0];
        String moduleKey = parts[1];
        String batchName = parts.length >= 3 ? parts[2] : "all." + type;
        
        return new BatchPluginResource(batchName, moduleKey, type, queryParams, url.substring(0, startIndex));
    }

    public static boolean matches(String url)
    {
        return url.indexOf(URL_PREFIX) != -1;
    }

    public String getUrl()
    {
        // e.g. /download/batch/css/example.plugin:webresources/webresources.css?ie=true
        StringBuffer sb = new StringBuffer(staticUrlPrefix);
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(type).append(PATH_SEPARATOR)
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
        return params;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
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