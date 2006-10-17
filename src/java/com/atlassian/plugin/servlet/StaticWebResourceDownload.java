package com.atlassian.plugin.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Handle resources of the type:
 *
 * <pre><code>{server root}/download/static/{build num}/{plugin version}/{system date}/include/path/to/includefile</code></pre>
 *
 * and redirect them to <pre><code>/path/to/includefile</code></pre>
 * <p>
 * Technically speaking, this plugin just strips everything before, and including '/include', adds caching headers,
 * and redirects to the resource.
 *
 * @see ResourceDownloadUtils#addCachingHeaders(javax.servlet.http.HttpServletResponse)
 */
public class StaticWebResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(StaticWebResourceDownload.class);
    private static final String STATIC_PATH_PREFIX = "static";
    private static final String INCLUDE_KEY_DELINEATOR = "/include/";

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BaseFileServerServlet.SERVLET_PATH + "/" + STATIC_PATH_PREFIX) != -1 &&
                urlPath.indexOf(INCLUDE_KEY_DELINEATOR) != -1;
    }

    public void serveFile(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        String includePath = splitIntoLibraryAndResource(httpServletRequest.getRequestURI(), servlet);
        if (includePath != null)
        {
            ResourceDownloadUtils.addCachingHeaders(httpServletResponse);
            try
            {
                // Redirect to "/path/to/includefile"
                httpServletRequest.getRequestDispatcher("/" + includePath).forward(httpServletRequest, httpServletResponse);
            }
            catch (ServletException e)
            {
                log.error(e);
                throw new IOException(e.getMessage());
            }
        }
        else
        {
            log.info("Invalid resource path spec: " + httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String splitIntoLibraryAndResource(String requestUri, BaseFileServerServlet servlet)
    {
        requestUri = servlet.urlDecode(requestUri);
        int afterTheResourcesString = requestUri.indexOf(INCLUDE_KEY_DELINEATOR);
        return requestUri.substring(afterTheResourcesString + INCLUDE_KEY_DELINEATOR.length());

    }

}
