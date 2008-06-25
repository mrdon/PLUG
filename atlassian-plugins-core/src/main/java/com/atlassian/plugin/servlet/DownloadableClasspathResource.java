package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class DownloadableClasspathResource extends AbstractDownloadableResource
{
    private static final Log log = LogFactory.getLog(DownloadableClasspathResource.class);

    public DownloadableClasspathResource(BaseFileServerServlet servlet, Plugin plugin, ResourceLocation resourceDescriptor, String extraPath)
    {
        super(servlet, plugin, resourceDescriptor, extraPath);
    }


    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        if (checkResourceNotModified(httpServletRequest, httpServletResponse))
            return;

        log.debug("Serving: " + this);
        InputStream resourceStream = plugin.getResourceAsStream(getLocation());
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
