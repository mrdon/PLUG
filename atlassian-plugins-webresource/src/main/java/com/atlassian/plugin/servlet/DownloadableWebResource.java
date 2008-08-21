package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DownloadableWebResource extends AbstractDownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableWebResource.class);

    public DownloadableWebResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ContentTypeResolver contentTypeResolver)
    {
        super(plugin, resourceLocation, extraPath, contentTypeResolver);
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
