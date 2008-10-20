package com.atlassian.plugin.resourcedownload.servlet;

import com.atlassian.plugin.resourcedownload.PluginResourceLocator;
import com.atlassian.plugin.resourcedownload.ContentTypeResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PluginResourceDownloadStrategy implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluginResourceDownloadStrategy.class);

    private String characterEncoding = "UTF-8"; // default to sensible encoding
    private PluginResourceLocator resourceLocator;
    private ContentTypeResolver contentTypeResolver;

    public PluginResourceDownloadStrategy(String characterEncoding, PluginResourceLocator resourceLocator, ContentTypeResolver contentTypeResolver)
    {
        this.characterEncoding = characterEncoding;
        this.resourceLocator = resourceLocator;
        this.contentTypeResolver = contentTypeResolver;
    }

    public boolean matches(String urlPath)
    {
        return resourceLocator.matches(urlPath);
    }

    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        try
        {
            String requestUri = URLDecoder.decode(request.getRequestURI(), characterEncoding);
            DownloadableResource downloadableResource = resourceLocator.locateByUrl(requestUri);

            if (downloadableResource == null)
            {
                log.info("Invalid resource path spec: " + request.getRequestURI());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if(downloadableResource.checkResourceNotModified(request, response))
            {
                response.setContentType(contentTypeResolver.getContentType(requestUri));
                downloadableResource.serveResource(request, response);
            }
        }
        catch(IOException e)
        {
            throw new DownloadException(e);
        }
    }
}
