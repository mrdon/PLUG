package com.atlassian.plugin.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResourceDownloadUtils
{
    private static final Log log = LogFactory.getLog(ResourceDownloadUtils.class);
    private static final long TEN_YEARS = 1000L * 60L * 60L * 24L *365L * 10L;

    public static void serveFileImpl(HttpServletResponse httpServletResponse, InputStream in) throws IOException
    {
        OutputStream out = httpServletResponse.getOutputStream();

        try
        {
            byte[] buffer = new byte[16 * 1024];
            int read_count;

            while ((read_count = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read_count);
            }

            log.info("Serving file done.");
        }
        catch (Exception e)
        {
            log.info("I/O Error serving the requested file: " + e.toString());
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                } catch (IOException e)
                {
                    // noop.
                }
            }
            if (out != null)
            {
                try
                {
                    out.flush();

                    //out.close();
                }
                catch (Exception e)
                {
                    log.info("Error flushing output stream: " + e.toString());
                }
            }
        }
    }

    /**
     * Set 'expire' headers to cache for ten years.
     */
    public static void addCachingHeaders(HttpServletResponse httpServletResponse)
    {
        httpServletResponse.setDateHeader("Expires", System.currentTimeMillis() + TEN_YEARS);
        httpServletResponse.setHeader("Cache-Control", "max-age=" + TEN_YEARS);
        httpServletResponse.addHeader("Cache-Control", "private");
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
