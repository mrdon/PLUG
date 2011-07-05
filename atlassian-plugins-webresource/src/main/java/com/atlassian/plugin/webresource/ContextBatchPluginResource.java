package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.DownloadException;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class ContextBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String CONTEXT_SEPARATOR = ",";

    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "contextbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "batch";

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String key;
    private final List<String> contexts;

    public ContextBatchPluginResource(String key, List<String> contexts, String type, Map<String, String> params)
    {
        this.resourceName = DEFAULT_RESOURCE_NAME_PREFIX + "." + type;
        this.delegate = new BatchPluginResource(null, type, params);
        this.key = key;
        this.contexts = contexts;
    }

    public List<String> getContexts()
    {
        return contexts;
    }


    public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        delegate.serveResource(request, response);
    }

    public void streamResource(OutputStream out) throws DownloadException
    {
        delegate.streamResource(out);
    }

    public String getContentType()
    {
        return delegate.getContentType();
    }

    public void add(DownloadableResource downloadableResource)
    {
        delegate.add(downloadableResource);
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getUrl()
    {
        StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR)
                .append(key).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(WebResourceIntegration integration)
    {
        return integration.getSuperBatchVersion();
    }

    public String getType()
    {
        return delegate.getType();
    }

    public boolean isCacheSupported()
    {
        return true;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public String getModuleCompleteKey()
    {
        return "contextbatch-" + resourceName;
    }

    @Override
    public String toString()
    {
        return "[Context Batch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }
}
