package com.atlassian.plugin.servlet;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.PluginAccessor;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableResource.class);

    private ResourceLocation resourceDescriptor;
    private String extraPath;
    private String pluginKey;
    private BaseFileServerServlet servlet;

    public DownloadableResource(BaseFileServerServlet servlet, String pluginKey, ResourceLocation resourceDescriptor, String extraPath)
    {
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

    public String getLocation()
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

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, PluginAccessor pluginAccessor) throws IOException
    {
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

}
