package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.util.LastModifiedHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

abstract class AbstractDownloadableResource implements DownloadableResource
{
    private static final Log log = LogFactory.getLog(AbstractDownloadableResource.class);

    protected Plugin plugin;
    protected String extraPath;
    protected ResourceLocation resourceLocation;

    public AbstractDownloadableResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath)
    {
        if (extraPath != null && !"".equals(extraPath.trim()) && !resourceLocation.getLocation().endsWith("/"))
        {
            extraPath = "/" + extraPath;
        }

        this.plugin = plugin;
        this.extraPath = extraPath;
        this.resourceLocation = resourceLocation;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        log.debug("Serving: " + this);

        InputStream resourceStream = getResourceAsStream();
        if (resourceStream == null)
        {
            log.warn("Resource not found: " + this);
            return;
        }

        if(StringUtils.isNotBlank(getContentType()))
        {
            response.setContentType(getContentType());
        }

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

    /**
     * Checks any "If-Modified-Since" header from the request against the plugin's loading time, since plugins can't
     * be modified after they've been loaded this is a good way to determine if a plugin resource has been modified
     * or not.
     *
     * If this method returns true, don't do any more processing on the request -- the response code has already been
     * set to "304 Not Modified" for you, and you don't need to serve the file.
     */
    public boolean isResourceModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }

    public String getContentType()
    {
        return resourceLocation.getContentType();
    }

    /**
     * Returns an {@link InputStream} to stream the resource from.
     */
    protected abstract InputStream getResourceAsStream();

    protected String getLocation()
    {
        return resourceLocation.getLocation() + extraPath;
    }

    public String toString()
    {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }
}
