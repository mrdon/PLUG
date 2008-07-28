package com.atlassian.plugin.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResourceDownloadUtils
{
    private static final Log log = LogFactory.getLog(ResourceDownloadUtils.class);
    private static final long TEN_YEARS = 1000L * 60L * 60L * 24L *365L * 10L;

    /**
     * @deprecated Since 2.0. Use {@link IOUtils#copy(InputStream, OutputStream)} instead. The method calling
     * this should be responsible for closing streams and flushing if necessary.
     */
    public static void serveFileImpl(HttpServletResponse httpServletResponse, InputStream in) throws IOException
    {
        OutputStream out = httpServletResponse.getOutputStream();
        try
        {
            IOUtils.copy(in, out);
        }
        catch (IOException e)
        {
            log.error("Error serving the requested file: " + e.getMessage());
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
                log.warn("Error flushing output stream: " + e.getMessage());
            }
        }
        log.info("Serving file done.");
    }


    /**
     * Set 'expire' headers to cache for ten years.
     */
    public static void addCachingHeaders(HttpServletResponse httpServletResponse)
    {
        if (!Boolean.getBoolean("atlassian.disable.caches"))
        {
            httpServletResponse.setDateHeader("Expires", System.currentTimeMillis() + TEN_YEARS);
            httpServletResponse.setHeader("Cache-Control", "max-age=" + TEN_YEARS);
            httpServletResponse.addHeader("Cache-Control", "private");
        }
    }

    /**
     * Set 'expire' headers to cache for ten years.  This method is called from UrlRewriteFilter, and therefore needs
     * to have 'request' and 'response' as parameters.
     *
     * @see <a href="http://tuckey.org/urlrewrite/manual/2.6/">http://tuckey.org/urlrewrite/manual/2.6/</a>
     */
    public static void addCachingHeaders(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        addCachingHeaders(httpServletResponse);
    }
}
