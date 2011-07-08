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
 * @since 2.9.0
 */
public class BatchSubResource implements DownloadableResource
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

    public String getResourceName()
    {
        return resourceName;
    }

    @Override
    public String toString()
    {
        return "[Subbatch module=" + delegate.getModuleCompleteKey() + " name=" + resourceName + "]";
    }
}
