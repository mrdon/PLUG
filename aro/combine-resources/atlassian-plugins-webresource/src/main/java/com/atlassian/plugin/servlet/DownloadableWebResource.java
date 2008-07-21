package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DownloadableWebResource extends AbstractDownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableWebResource.class);

    /**
     * @deprecated Since 2.0. Use {@link #DownloadableWebResource(Plugin, ResourceLocation, String, ApplicationDownloadContext)} instead.
     */
    public DownloadableWebResource(BaseFileServerServlet servlet, Plugin plugin, ResourceLocation resourceDescriptor, String extraPath)
    {
        super(servlet, plugin, resourceDescriptor, extraPath);
    }

    public DownloadableWebResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ApplicationDownloadContext context)
    {
        super(plugin, resourceLocation, extraPath, context);
    }

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        try
        {
            httpServletResponse.setContentType(getContentType()); // this will be used if content-type is not set by the forward handler, e.g. for webapp content in Tomcat
            httpServletRequest.getRequestDispatcher(getLocation()).forward(httpServletRequest, httpServletResponse);
        }
        catch (ServletException e)
        {
            log.error(e);
            throw new IOException(e.getMessage());
        }
    }
}
