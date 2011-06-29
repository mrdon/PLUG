package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.DownloadException;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static com.atlassian.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ContextBatchPluginResource implements DownloadableResource, BatchResource, PluginResource
{
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "contextbatch" + PATH_SEPARATOR;

    private final BatchPluginResource delegate;
    private final String resourceName;
    private List<String> contexts;

    public static boolean matches(String path)
    {
        return path.indexOf(URL_PREFIX) != -1;
    }

    public static ContextBatchPluginResource parse(String path, Map<String, String> params)
    {
        final int fullStopIndex = path.lastIndexOf(".");
        final int slashIndex = path.lastIndexOf("/");
        String type = path.substring(fullStopIndex + 1);
        String resourceName = path.substring(slashIndex + 1, fullStopIndex);
        return new ContextBatchPluginResource(resourceName, type, params);
    }

    public ContextBatchPluginResource(String resourceName, String type, Map<String, String> params)
    {
        this.resourceName = resourceName + "." + type;
        this.delegate = new BatchPluginResource(null, type, params);
    }

    public List<String> getContexts()
    {
        if (contexts == null)
        {
            contexts = new ArrayList<String>();

            String temp = resourceName.substring(0, resourceName.lastIndexOf("."));
            StringTokenizer st = new StringTokenizer(temp, "+ ");
            while (st.hasMoreTokens())
            {
                contexts.add(st.nextToken());
            }
        }
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
        // TODO - work out a better naming scheme.
        String encodedName;
        try
        {
            encodedName = URLEncoder.encode(URLEncoder.encode(resourceName, "UTF-8"), "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            encodedName = resourceName;
        }
        StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR)
                .append(encodedName);
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
