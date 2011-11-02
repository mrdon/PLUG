package com.atlassian.plugin.webresource;

import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.util.concurrent.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;


/**
 * Represents a batch of all resources that declare themselves as part of a given context(s).
 * <p/>
 * The URL for batch resources is /download/contextbatch/&lt;type>/&lt;contextname>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 *
 * @since 2.9.0
 */
class ContextBatchPluginResource implements DownloadableResource, BatchResource, CacheablePluginResource
{
    static final String CONTEXT_SEPARATOR = ",";

    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "contextbatch" + PATH_SEPARATOR;

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final String key;
    private final Iterable<String> contexts;
    private final String hash;
    private final FileCacheKey cacheKey;
    private final FileCache fileCache;

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params, final FileCache fileCache, final FileCacheKey cacheKey)
    {
        this(key, contexts, hash, type, params, Collections.<DownloadableResource>emptyList(), fileCache, cacheKey);
    }

    ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, final FileCache fileCache, final FileCacheKey cacheKey)
    {
        this(key, contexts, null, type, params, resources, fileCache, cacheKey);
    }

    private ContextBatchPluginResource(final String key, final Iterable<String> contexts, final String hash, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, @NotNull final FileCache fileCache, @NotNull final FileCacheKey cacheKey)
    {
        resourceName = key + "." + type;
        delegate = new BatchPluginResource(null, type, params, resources);
        this.key = key;
        this.contexts = contexts;
        this.hash = hash;
        this.cacheKey = cacheKey;
        this.fileCache = fileCache;
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
        try
        {
            fileCache.stream(cacheKey, response.getOutputStream(), delegate);
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        fileCache.stream(cacheKey, out, delegate);
    }

    public String getContentType()
    {
        return delegate.getContentType();
    }

    boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public String getCacheUrl(WebResourceIntegration integration)
    {
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(integration.getStaticResourceLocale()).append(PATH_SEPARATOR).append(hash).append(PATH_SEPARATOR).append(key).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public String getUrl()
    {
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(hash).append(PATH_SEPARATOR).append(key).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(final WebResourceIntegration integration)
    {
        return integration.getSystemBuildNumber(); //we use the hash in the part that will not get stripped.
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
