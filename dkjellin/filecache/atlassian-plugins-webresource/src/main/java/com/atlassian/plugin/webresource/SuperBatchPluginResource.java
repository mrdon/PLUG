package com.atlassian.plugin.webresource;

import com.atlassian.plugin.cache.filecache.FileCache;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;

/**
 * Creates a batch of all like-typed resources that are declared as "super-batch="true"" in their plugin
 * definitions.
 * <p/>
 * The URL for batch resources is /download/superbatch/&lt;type>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 */
public class SuperBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String URL_PREFIX = PATH_SEPARATOR + "superbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "batch";

    private final BatchPluginResource delegate;
    private final String resourceName;
    private final FileCache fileCache;
    private final String url;

    public static SuperBatchPluginResource createBatchFor(final PluginResource pluginResource)
    {
        return new SuperBatchPluginResource(ResourceUtils.getType(pluginResource.getResourceName()), pluginResource.getParams());
    }

    /**
     * Creates a super batch resource without the included resources
     *
     * @param type   the type of resource (CSS/JS)
     * @param params the parameters (ieOnly,media)
     */
    public SuperBatchPluginResource(final String type, final Map<String, String> params)
    {
        this(type, params, Collections.<DownloadableResource>emptyList(), null, "invalid");
    }

    public SuperBatchPluginResource(final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, FileCache fileCache, String path)
    {
        this(DEFAULT_RESOURCE_NAME_PREFIX + "." + type, type, params, resources, fileCache, path);
    }

    protected SuperBatchPluginResource(final String resourceName, final String type, final Map<String, String> params, final Iterable<DownloadableResource> resources, FileCache fileCache, String path)
    {
        this.resourceName = resourceName;
        delegate = new BatchPluginResource(null, type, params, resources);
        this.fileCache = fileCache;
        this.url = path;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        if (fileCache == null)
        {
            delegate.serveResource(request, response);
            return;
        }
        try
        {
            fileCache.stream(url, response.getOutputStream(), delegate);
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        if(fileCache == null)
        {
            delegate.streamResource(out);
            return;
        }
        try
        {
            fileCache.stream(url, out, delegate);
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }
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
        final StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams()
    {
        return delegate.getParams();
    }

    public String getVersion(final WebResourceIntegration integration)
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
        return "superbatch";
    }

    @Override
    public String toString()
    {
        return "[Superbatch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }
}
