package com.atlassian.plugin.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.atlassian.plugin.PluginAccessor;

/**
 *  * <code>{server root}/download/static/{build num}/{plugin version}/{system date}/pluginKey/{plugin key}:{module key}/{resource name}</code>
 */
public class PluginStaticResourceDownload extends PluginResourceDownload
{
    private static final Log log = LogFactory.getLog(PluginStaticResourceDownload.class);
    public static final String STATIC_PATH_PREFIX = "static";
    public static final String PLUGIN_KEY_DELINEATOR = "pluginKey";
    private static final long ONE_MONTH = 1000L * 60L * 60L * 24L *31L;

    public PluginStaticResourceDownload(PluginAccessor pluginAccessor)
    {
        super(pluginAccessor); // constructor for JIRA
    }

    public PluginStaticResourceDownload()
    {
        //no arg constructor for Confluence
    }

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BaseFileServerServlet.SERVLET_PATH + "/" + STATIC_PATH_PREFIX) != -1;
    }

    public void serveFile(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        String[] parts = splitIntoLibraryAndResource(httpServletRequest.getRequestURI(), servlet);
        if (parts.length == 2)
        {
            httpServletResponse.setDateHeader("Expires", System.currentTimeMillis() + ONE_MONTH);
            httpServletResponse.setHeader("Cache-Control", "max-age=" + ONE_MONTH);
            httpServletResponse.addHeader("Cache-Control", "private");
            servePluginResource(servlet, httpServletRequest, httpServletResponse, parts[0], parts[1]);
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
        requestUri = requestUri.substring(afterTheResourcesString + PLUGIN_KEY_DELINEATOR.length() + 1);
        return requestUri.split("/", 2);
    }

}
