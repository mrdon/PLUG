package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a batch of all resources that declare themselves as part of a given context(s).
 *
 * The URL for batch resources is /download/contextbatch/&lt;type>/&lt;contextname>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 *
 * @since 2.9.0
 */
class ContextBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String CONTEXT_SEPARATOR = ",";

    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "contextbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "batch";

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String key;
    private final Iterable<String> contexts;
    private String hash;

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params)
    {
        this(key, contexts, hash, type, params, Collections.<DownloadableResource> emptyList());
    }

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources)
    {
        this(key, contexts, null, type, params, resources);
    }

    private ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources)
    {
        resourceName = DEFAULT_RESOURCE_NAME_PREFIX + "." + type;
        delegate = new BatchPluginResource(null, type, params, resources);
        this.key = key;
        this.contexts = contexts;
        this.hash = hash;
    }

    Iterable<String> getContexts()
    {
        return contexts;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        delegate.serveResource(request, response);
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        delegate.streamResource(out);
    }

    public String getContentType()
    {
        return delegate.getContentType();
    }

    boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getUrl()
    {
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(key).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(final WebResourceIntegration integration)
    {
        return hash;
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
