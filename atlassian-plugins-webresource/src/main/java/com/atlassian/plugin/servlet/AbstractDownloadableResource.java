package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.util.LastModifiedHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This base class is used to provide the ability to server minified versions of files
 * if required and available.
 *
 * @since 2.2
 */
abstract class AbstractDownloadableResource implements DownloadableResource
{
    private static final Log log = LogFactory.getLog(AbstractDownloadableResource.class);

    /**
     * This is a the system environment variable to set to disable the minification naming strategy used to find web
     * resources.
     */
    private static final String ATLASSIAN_WEBRESOURCE_DISABLE_MINIFICATION = "atlassian.webresource.disable.minification";

    protected Plugin plugin;
    protected String extraPath;
    protected ResourceLocation resourceLocation;
    private final boolean disableMinification;

    public AbstractDownloadableResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath)
    {
        this(plugin, resourceLocation, extraPath, false);
    }

    public AbstractDownloadableResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, boolean disableMinification)
    {
        if (extraPath != null && !"".equals(extraPath.trim()) && !resourceLocation.getLocation().endsWith("/"))
        {
            extraPath = "/" + extraPath;
        }
        this.disableMinification = disableMinification;
        this.plugin = plugin;
        this.extraPath = extraPath;
        this.resourceLocation = resourceLocation;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        log.debug("Serving: " + this);

        InputStream resourceStream = getResourceAsStreamViaMinificationStrategy();
        if (resourceStream == null)
        {
            log.warn("Resource not found: " + this);
            return;
        }

        final String contentType = getContentType();
        if(StringUtils.isNotBlank(contentType))
        {
            response.setContentType(contentType);
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

        streamResource(resourceStream, out);
        log.debug("Serving file done.");
    }

    public void streamResource(OutputStream out)
    {
        InputStream resourceStream = getResourceAsStreamViaMinificationStrategy();
        if (resourceStream == null)
        {
            log.warn("Resource not found: " + this);
            return;
        }

        streamResource(resourceStream, out);
    }

    /**
     * Copy from the supplied OutputStream to the supplied InputStream. Note that the InputStream will be closed on
     * completion.
     *
     * @param in the stream to read from
     * @param out the stream to write to
     */
    private void streamResource(InputStream in, OutputStream out)
    {
        try
        {
            IOUtils.copy(in, out);
        }
        catch (IOException e)
        {
            log.error("Error serving the requested file", e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
            try
            {
                out.flush();
            }
            catch (IOException e)
            {
                log.warn("Error flushing output stream", e);
            }
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
     * Returns an {@link InputStream} to stream the resource from based on resource name.
     *
     * @param resourceLocation the location of the resource to try and load
     *
     * @return an InputStream if the resource can be found or null if cant be found
     */
    protected abstract InputStream getResourceAsStream(String resourceLocation);

    /**
     * This is called to return the location of the resource that this object represents.
     *
     * @return the location of the resource that this object represents.
     */
    protected String getLocation()
    {
        return resourceLocation.getLocation() + extraPath;
    }

    @Override
    public String toString()
    {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }

    /**
     * This is called to use a minification naming strategy to find resources.  If a minified file cant by found then
     * the base location is ised as the fall back
     *
     * @return an InputStream r null if nothing can be found for the resource name
     */
    private InputStream getResourceAsStreamViaMinificationStrategy()
    {

        InputStream inputStream = null;
        String location = getLocation();
        if (minificationStrategyInPlay(location))
        {
            final String minifiedLocation = getMinifiedLocation(location);
            inputStream = getResourceAsStream(minifiedLocation);
        }
        if (inputStream == null)
        {
            inputStream = getResourceAsStream(location);
        }
        return inputStream;
    }


    /**
     * Returns true if the minification strategy should be applied to a given resource name
     *
     * @param resourceLocation the location of the resource
     *
     * @return true if the minification strategy should be used.
     */
    private boolean minificationStrategyInPlay(final String resourceLocation)
    {
        // check if minification has been turned off for this resource (at the module level)
        if (disableMinification)
        {
            return false;
        }

        // secondly CHECK if we have a System property set to true that DISABLES the minification
        try
        {
            if (Boolean.getBoolean(ATLASSIAN_WEBRESOURCE_DISABLE_MINIFICATION))
            {
                return false;
            }
        }
        catch (SecurityException se)
        {
            // some app servers might have protected access to system properties.  Unlikely but lets be defensive
        }
        // We only minify .js or .css files
        if (resourceLocation.endsWith(".js"))
        {
            // Check if it is already the minified vesrion of the file
            return !(resourceLocation.endsWith("-min.js") || resourceLocation.endsWith(".min.js"));
        }
        if (resourceLocation.endsWith(".css")) 
        {
            // Check if it is already the minified vesrion of the file
            return !(resourceLocation.endsWith("-min.css") || resourceLocation.endsWith(".min.css"));
        }
        // Not .js or .css, don't bother trying to find a minified version (may save some file operations)
        return false;
    }

    private String getMinifiedLocation(String location)
    {
        int lastDot = location.lastIndexOf(".");
        // this can never but -1 since the method call is protected by a call to minificationStrategyInPlay() first
        return location.substring(0, lastDot) + "-min" + location.substring(lastDot);
    }
}
