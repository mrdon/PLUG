package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

public class DownloadableWebResource extends AbstractDownloadableResource
{
    public DownloadableWebResource(BaseFileServerServlet servlet, Plugin plugin, ResourceLocation resourceDescriptor, String extraPath)
    {
        super(servlet, plugin, resourceDescriptor, extraPath);
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
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }
}
