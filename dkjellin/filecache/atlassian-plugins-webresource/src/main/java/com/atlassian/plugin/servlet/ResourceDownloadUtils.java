package com.atlassian.plugin.servlet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceDownloadUtils
{
    private static final Logger log = LoggerFactory.getLogger(ResourceDownloadUtils.class);
    private static final long ONE_YEAR_SECONDS = 60L * 60L * 24L * 365L;
    private static final long ONE_YEAR_MILLISECONDS = 1000 * ONE_YEAR_SECONDS;

    /**
     * @deprecated Since 2.0. Use {@link IOUtils#copy(InputStream, OutputStream)} instead. The method calling
     * this should be responsible for closing streams and flushing if necessary.
     */
    @Deprecated
    public static void serveFileImpl(final HttpServletResponse httpServletResponse, final InputStream in) throws IOException
    {
        final OutputStream out = httpServletResponse.getOutputStream();
        try
        {
            IOUtils.copy(in, out);
        }
        finally
        {
            IOUtils.closeQuietly(in);
            out.flush();
        }
        log.debug("Serving file done.");
    }

    /**
     * Set 'expire' headers to cache for one year. Also adds the additional cache control values passed in.
     * Note, this method resets the cache control headers if set previously. 
     */
    public static void addCachingHeaders(final HttpServletResponse httpServletResponse, final String... cacheControls)
    {
        if (!Boolean.getBoolean("atlassian.disable.caches"))
        {
            httpServletResponse.setDateHeader("Expires", System.currentTimeMillis() + ONE_YEAR_MILLISECONDS);
            httpServletResponse.setHeader("Cache-Control", "max-age=" + ONE_YEAR_SECONDS);
            for (final String cacheControl : cacheControls)
            {
                httpServletResponse.addHeader("Cache-Control", cacheControl);
            }
        }
    }

    /**
     * Set 'expire' headers to cache for one year, with public caching turned on.
     *
     * @deprecated Please use {@link #addPublicCachingHeaders(HttpServletRequest, HttpServletResponse)} or
     * {@link #addPrivateCachingHeaders(HttpServletRequest, HttpServletResponse)} instead.
     */
    @Deprecated
    public static void addCachingHeaders(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
    {
        addPublicCachingHeaders(httpServletRequest, httpServletResponse);
    }

    /**
     * Sets caching headers with public cache control. Applications should call this method from urlrewrite.xml to
     * decorate urls like <code>/s/{build num}/.../_/resourceurl</code>.
     *
     * @see <a href="http://tuckey.org/urlrewrite/manual/2.6/">http://tuckey.org/urlrewrite/manual/2.6/</a>
     */
    public static void addPublicCachingHeaders(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
    {
        addCachingHeaders(httpServletResponse, "public");
    }

    /**
     * Sets caching headers with private cache control. Applications should call this method from urlrewrite.xml to
     * decorate urls like <code>/sp/{build num}/.../_/resourceurl</code>.
     *
     * @see <a href="http://tuckey.org/urlrewrite/manual/2.6/">http://tuckey.org/urlrewrite/manual/2.6/</a>
     */
    public static void addPrivateCachingHeaders(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
    {
        addCachingHeaders(httpServletResponse, "private");
    }
}
