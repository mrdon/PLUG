package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.DownloadException;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Map;

/**
 * Creates a batch of all like-typed resources that are declared as "super-batch="true"" in their plugin
 * definitions.
 *
 * The URL for batch resources is /download/superbatch/&lt;type>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 *
 */
public class SuperBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "superbatch" + PATH_SEPARATOR;
    static final String RESOURCE_NAME_PREFIX = "batch";

    private BatchPluginResource delegate;

    public static boolean matches(String path)
    {
        return path.indexOf(URL_PREFIX) != -1;
    }

    public static SuperBatchPluginResource createBatchFor(PluginResource pluginResource)
    {
        String type = pluginResource.getResourceName().substring(pluginResource.getResourceName().lastIndexOf(".") + 1);
        return new SuperBatchPluginResource(type, pluginResource.getParams());
    }

    public static SuperBatchPluginResource parse(String path, Map<String, String> params)
    {
        String type = path.substring(path.lastIndexOf(".") + 1);
        return new SuperBatchPluginResource(type, params);
    }

    public SuperBatchPluginResource(String type, Map<String, String> params)
    {
        this.delegate = new BatchPluginResource(null, type, params);
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

    public String getUrl()
    {
        StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(RESOURCE_NAME_PREFIX).append(".").append(getType());
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
        return "batch" + "." + getType();
    }

    public String getModuleCompleteKey()
    {
        return "superbatch";
    }
}