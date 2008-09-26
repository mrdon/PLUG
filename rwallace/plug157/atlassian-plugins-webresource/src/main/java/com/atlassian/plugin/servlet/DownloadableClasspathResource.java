package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DownloadableClasspathResource extends AbstractDownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableClasspathResource.class);

    public DownloadableClasspathResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ContentTypeResolver context)
    {
        super(plugin, resourceLocation, extraPath, context);
    }

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        if (checkResourceNotModified(httpServletRequest, httpServletResponse))
            return;

        log.debug("Serving: " + this);
        InputStream resourceStream = plugin.getResourceAsStream(getLocation());
        if (resourceStream == null)
        {
            log.info("Resource not found: " + this);
            return;
        }

        // overwrite caching headers with private cache-control
        if("true".equalsIgnoreCase(resourceLocation.getParameter("cache-private")))
        {
            ResourceDownloadUtils.addCachingHeaders(httpServletResponse, "private");    
        }

        httpServletResponse.setContentType(getContentType());
        OutputStream out = httpServletResponse.getOutputStream();
        try
        {
            IOUtils.copy(resourceStream, out);
        }
        catch (IOException e)
        {
            log.error("Error serving the requested file", e);
        }
        finally
        {
            IOUtils.closeQuietly(resourceStream);
            try
            {
                out.flush();
            }
            catch (IOException e)
            {
                log.warn("Error flushing output stream", e);
            }
        }
        log.info("Serving file done.");
    }
}
