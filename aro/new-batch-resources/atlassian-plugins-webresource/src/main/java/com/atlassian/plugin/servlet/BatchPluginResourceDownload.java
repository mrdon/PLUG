package com.atlassian.plugin.servlet;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.atlassian.plugin.webresource.PluginResource;
import com.atlassian.plugin.webresource.batch.BatchResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

public class BatchPluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(BatchPluginResourceDownload.class);

    private PluginAccessor pluginAccessor;
    private ContentTypeResolver contentTypeResolver;
    private String characterEncoding = "UTF-8";

    public BatchPluginResourceDownload(PluginAccessor pluginAccessor, ContentTypeResolver contentTypeResolver)
    {
        this.pluginAccessor = pluginAccessor;
        this.contentTypeResolver = contentTypeResolver;
    }

    public BatchPluginResourceDownload(PluginAccessor pluginAccessor, ContentTypeResolver contentTypeResolver, String characterEncoding)
    {
        this.characterEncoding = characterEncoding;
        this.pluginAccessor = pluginAccessor;
        this.contentTypeResolver = contentTypeResolver;
    }

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BatchResource.URL_PREFIX) != -1;
    }

    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        try
        {
            String url = URLDecoder.decode(request.getRequestURI(), characterEncoding);
            BatchResource batchResource = BatchResource.parse(url);

            if (batchResource != null)
            {
                response.setContentType(contentTypeResolver.getContentType(url));
                servePluginResource(batchResource, request, response);
            }
            else
            {
                log.info("Invalid batch resource path spec: " + url);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }
    }

    private void servePluginResource(BatchResource batchResource, HttpServletRequest request, HttpServletResponse response) throws IOException, DownloadException
    {
        String moduleCompleteKey = batchResource.getModuleCompleteKey();
        ModuleDescriptor moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);

        if(moduleDescriptor == null || !(moduleDescriptor instanceof WebResourceModuleDescriptor))
        {
            log.info("Plugin Resource module not found for: " + batchResource.getModuleCompleteKey());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        WebResourceModuleDescriptor webResourceModuleDescriptor = (WebResourceModuleDescriptor) moduleDescriptor;
        List<PluginResource> pluginResources = webResourceModuleDescriptor.getPluginResources(batchResource);

        for(PluginResource pluginResource : pluginResources)
        {
            log.debug("[" + batchResource.getModuleCompleteKey() + "] including resource in batch: " + pluginResource.getResourceName());
            DownloadableResource resource = getDownloadableResource(webResourceModuleDescriptor, pluginResource);
            if(resource == null)
            {
                log.info("[" + batchResource.getModuleCompleteKey() + "] Skipping for batch, could not locate resource: " + pluginResource.getResourceName());
                continue;
            }
            resource.serveResource(request, response);
        }
    }

    private DownloadableResource getDownloadableResource(WebResourceModuleDescriptor webResourceModuleDescriptor, PluginResource pluginResource)
    {
        Plugin plugin = webResourceModuleDescriptor.getPlugin();
        ResourceLocation resourceLocation = webResourceModuleDescriptor.getResourceLocation(PluginResourceDownload.DOWNLOAD_RESOURCE, pluginResource.getResourceName());
        if(resourceLocation == null)
            return null;

        // this allows plugins that are loaded from the web to be served
        if ("webContext".equalsIgnoreCase(resourceLocation.getParameter("source")))
            return new DownloadableWebResource(plugin, resourceLocation, "", contentTypeResolver);

        return new DownloadableClasspathResource(plugin, resourceLocation, "", contentTypeResolver);
    }

    public void setCharacterEncoding(String characterEncoding)
    {
        this.characterEncoding = characterEncoding;
    }
}
