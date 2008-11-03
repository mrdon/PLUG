package com.atlassian.plugin.servlet;

import com.atlassian.plugin.util.LastModifiedHandler;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public abstract class AbstractDownloadableResource implements DownloadableResource
{
    protected ResourceLocation resourceLocation;
    protected String extraPath;
    protected Plugin plugin;
    protected BaseFileServerServlet servlet;

    public AbstractDownloadableResource(BaseFileServerServlet servlet, Plugin plugin, ResourceLocation resourceLocation, String extraPath)
    {
        if (extraPath != null && !"".equals(extraPath.trim()) && !resourceLocation.getLocation().endsWith("/"))
        {
            extraPath = "/" + extraPath;
        }

        this.resourceLocation = resourceLocation;
        this.extraPath = extraPath;
        this.plugin = plugin;
        this.servlet = servlet;
    }


    protected String getContentType()
    {
        if (resourceLocation.getContentType() == null)
        {
            return servlet.getContentType(getLocation());
        }

        return resourceLocation.getContentType();
    }

    protected String getLocation()
    {
        return resourceLocation.getLocation() + extraPath;
    }

    public String toString()
    {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }

    public abstract void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException;

    /**
     * Checks any "If-Modified-Since" header from the request against the plugin's loading time, since plugins can't
     * be modified after they've been loaded this is a good way to determine if a plugin resource has been modified
     * or not.
     *
     * If this method returns true, don't do any more processing on the request -- the response code has already been
     * set to "304 Not Modified" for you, and you don't need to serve the file.
     */
    protected boolean checkResourceNotModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }
}