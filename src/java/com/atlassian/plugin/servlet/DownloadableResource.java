package com.atlassian.plugin.servlet;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.util.LastModifiedHandler;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableResource.class);

    private ResourceLocation resourceDescriptor;
    private String extraPath;
    private String pluginKey;
    private BaseFileServerServlet servlet;
    private final PluginAccessor pluginAccessor;

    public DownloadableResource(PluginAccessor pluginAccessor, BaseFileServerServlet servlet, String pluginKey, ResourceLocation resourceDescriptor, String extraPath)
    {
        this.pluginAccessor = pluginAccessor;
        if (extraPath != null && !"".equals(extraPath.trim()) && !resourceDescriptor.getLocation().endsWith("/"))
        {
            extraPath = "/" + extraPath;
        }

        this.resourceDescriptor = resourceDescriptor;
        this.extraPath = extraPath;
        this.pluginKey = pluginKey;
        this.servlet = servlet;
    }

    private String getContentType()
    {
        if (resourceDescriptor.getContentType() == null)
        {
            return servlet.getContentType(getLocation());
        }

        return resourceDescriptor.getContentType();
    }

    private String getLocation()
    {
        return resourceDescriptor.getLocation() + extraPath;
    }

    public String toString()
    {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }

    public String getPluginKey()
    {
        return pluginKey;
    }

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        if (checkResourceNotModified(httpServletRequest, httpServletResponse))
            return;

        log.debug("Serving: " + this);
        InputStream resourceStream = pluginAccessor.getPluginResourceAsStream(getPluginKey(), getLocation());
        if (resourceStream != null)
        {
            httpServletResponse.setContentType(getContentType());
            ResourceDownloadUtils.serveFileImpl(httpServletResponse, resourceStream);

            try
            {
                resourceStream.close();
            }
            catch (IOException e)
            {
                log.error("Could not close input stream on resource:", e);
            }

        }
        else
        {
            log.info("Resource not found: " + this);
        }
    }

    /**
     * Checks any "If-Modified-Since" header from the request against the plugin's loading time, since plugins can't
     * be modified after they've been loaded this is a good way to determine if a plugin resource has been modified
     * or not.
     *
     * If this method returns true, don't do any more processing on the request -- the response code has already been
     * set to "304 Not Modified" for you, and you don't need to serve the file.
     */
    private boolean checkResourceNotModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        Plugin plugin = pluginAccessor.getPlugin(getPluginKey());
        Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }


}
