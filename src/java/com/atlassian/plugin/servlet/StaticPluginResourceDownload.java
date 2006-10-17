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
 * <pre><code>{server root}/download/static/{build num}/{plugin version}/{system date}/pluginkey/{plugin key}:{module key}/{resource name}</code></pre>
 *
 * and redirect them to <pre><code>/download/resources/{plugin key}:{module key}/{resource name}</code></pre>
 * <p>
 * Technically speaking, this plugin just ensures that '/pluginkey' exists before the {plugin key} adds caching headers,
 * and redirect to the resource.
 *
 * @see ResourceDownloadUtils#addCachingHeaders(javax.servlet.http.HttpServletResponse)
 */
public class StaticPluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(StaticPluginResourceDownload.class);
    private static final String STATIC_PATH_PREFIX = "static";
    private static final String PLUGIN_KEY_DELINEATOR = "/pluginkey/";

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BaseFileServerServlet.SERVLET_PATH + "/" + STATIC_PATH_PREFIX) != -1 &&
                urlPath.indexOf(PLUGIN_KEY_DELINEATOR.toLowerCase()) != -1;
    }

    public void serveFile(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        String[] parts = splitIntoLibraryAndResource(httpServletRequest.getRequestURI(), servlet);
        if (parts.length == 2)
        {
            ResourceDownloadUtils.addCachingHeaders(httpServletResponse);

            try
            {
                // Redirect to "/download/resources/{plugin key}:{module key}/{resource name}"
                httpServletRequest.getRequestDispatcher("/download/resources/" + parts[0] + "/" + parts[1]).forward(httpServletRequest, httpServletResponse);
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

    private String[] splitIntoLibraryAndResource(String requestUri, BaseFileServerServlet servlet)
    {
        requestUri = servlet.urlDecode(requestUri);
        int afterTheResourcesString = requestUri.indexOf(PLUGIN_KEY_DELINEATOR);
        requestUri = requestUri.substring(afterTheResourcesString + PLUGIN_KEY_DELINEATOR.length());
        return requestUri.split("/", 2);
    }

}
