package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a plugin resource that is a subordinate of a batch.
 *
 * This is typically the case for CSS in the superbatch or context batch with relative urls to images. For example:
 * <code>/download/superbatch/css/images/foo.png</code>
 * <code>/download/contextbatch/css/contexta/images/foo.png</code>
 * @since 2.10
 */
public class BatchSubResource implements BatchResource, DownloadableResource, PluginResource
{
    private BatchPluginResource delegate;
    private String resourceName;

    public BatchSubResource(String resourceName, String type, Map<String, String> params)
    {
        this(resourceName, type, params, Collections.<DownloadableResource>emptyList());
    }

    public BatchSubResource(String resourceName, String type, Map<String, String> params, List<DownloadableResource> resources)
    {
        this.resourceName = resourceName;
        this.delegate = new BatchPluginResource(resourceName, type, params, resources);
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

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getUrl()
    {
        throw new UnsupportedOperationException("A sub batch resource should only be served not requested.");
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(WebResourceIntegration integration)
    {
        throw new UnsupportedOperationException("A sub batch resource should only be served not requested.");
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
        return "subbatch";
    }

    @Override
    public String toString()
    {
        return "[Subbatch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }
}
