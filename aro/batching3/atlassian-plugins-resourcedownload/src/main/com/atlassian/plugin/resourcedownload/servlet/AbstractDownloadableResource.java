package com.atlassian.plugin.resourcedownload.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.resourcedownload.util.LastModifiedHandler;
import com.atlassian.plugin.resourcedownload.servlet.DownloadException;
import com.atlassian.plugin.resourcedownload.servlet.DownloadableResource;
import com.atlassian.plugin.resourcedownload.ContentTypeResolver;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public abstract class AbstractDownloadableResource implements DownloadableResource
{
    private static final Log log = LogFactory.getLog(AbstractDownloadableResource.class);

    protected Plugin plugin;
    protected String extraPath;
    protected ResourceLocation resourceLocation;
    protected final ContentTypeResolver contentTypeResolver;

    public AbstractDownloadableResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ContentTypeResolver contentTypeResolver)
    {
        if (extraPath != null && !"".equals(extraPath.trim()) && !resourceLocation.getLocation().endsWith("/"))
        {
            extraPath = "/" + extraPath;
        }

        this.plugin = plugin;
        this.extraPath = extraPath;
        this.resourceLocation = resourceLocation;
        this.contentTypeResolver = contentTypeResolver;
    }

    protected String getContentType()
    {
        if (resourceLocation.getContentType() == null)
        {
            return contentTypeResolver.getContentType(getLocation());
        }

        return resourceLocation.getContentType();
    }

    protected String getLocation()
    {
        return resourceLocation.getLocation() + extraPath;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        if (checkResourceNotModified(request, response))
            return;

        log.debug("Serving: " + this);

        InputStream resourceStream = getResourceAsStream();
        if (resourceStream == null)
        {
            log.info("Resource not found: " + this);
            return;
        }

        response.setContentType(getContentType());
        OutputStream out;
        try
        {
            out = response.getOutputStream();
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }

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

    protected abstract InputStream getResourceAsStream();

    public boolean checkResourceNotModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }

    public String toString()
    {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }
}
