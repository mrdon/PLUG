package com.atlassian.plugin.servlet;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.atlassian.plugin.webresource.ResourceUrlFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

/**
 * Supports the download of mutiple plugin resources of the same file type. File types are determined by the resources
 * matching the supported extensions.
 * <p/>
 * The URL that it parses looks like this: <br> <code>{server root}/download/resources/{file type}/{plugin key}:{module key}
 */
public class BatchPluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(BatchPluginResourceDownload.class);

    /**
     * The resource extension that is supported by this servlet. E.g. js, css etc.
     */
    private String supportedExtension;
    private String characterEncoding = "UTF-8"; // default to sensible encoding
    private String contentType;
    private PluginAccessor pluginAccessor;
    private ResourceUrlParser resourceUrlParser;

    public BatchPluginResourceDownload(String supportedExtension, String contentType, String characterEncoding,
        PluginAccessor pluginAccessor)
    {
        this.supportedExtension = supportedExtension;
        this.characterEncoding = characterEncoding;
        this.contentType = contentType;
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(String urlPath)
    {
        return getResourceUrlParser().matches(urlPath);
    }

    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        try
        {
            String requestUri = URLDecoder.decode(request.getRequestURI(), characterEncoding);
            String completeKey = getResourceUrlParser().getResourcePart(requestUri);

            if (completeKey == null || StringUtils.isBlank(completeKey))
            {
                log.info("Invalid plugin resource path spec: " + request.getRequestURI());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            List<ResourceDescriptor> resources = getResourceDescriptors(completeKey);
            if(resources == null)
            {
                log.info("Plugin Resources not found: " + completeKey);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            servePluginResources(completeKey, resources, request, response);
        }
        catch(IOException e)
        {
            throw new DownloadException(e);
        }
    }

    private ResourceUrlParser getResourceUrlParser()
    {
        if(resourceUrlParser == null)
        {
            resourceUrlParser = new ResourceUrlParser(AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + supportedExtension);
        }
        return resourceUrlParser;
    }

    private List<ResourceDescriptor> getResourceDescriptors(String moduleCompleteKey)
    {
        // resources from web module
        if (moduleCompleteKey.indexOf(":") > -1)
        {
            ModuleDescriptor moduleDescriptor = pluginAccessor.getPluginModule(moduleCompleteKey);
            if(!(moduleDescriptor instanceof WebResourceModuleDescriptor) || !pluginAccessor.isPluginModuleEnabled(moduleCompleteKey))
            {
                return null;
            }
            WebResourceModuleDescriptor webResourceModuleDescriptor = (WebResourceModuleDescriptor) moduleDescriptor;
            return webResourceModuleDescriptor.getResourceDescriptors(PluginResourceDownload.DOWNLOAD_RESOURCE);
        }

        // resources from the plugin
        Plugin plugin = pluginAccessor.getPlugin(moduleCompleteKey);
        if(plugin == null || !plugin.isEnabled())
        {
            return null;
        }
        return plugin.getResourceDescriptors(PluginResourceDownload.DOWNLOAD_RESOURCE);
    }

    private void servePluginResources(String completeKey, List<ResourceDescriptor> resources, HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        response.setContentType(contentType);

        for(ResourceDescriptor resourceDescriptor : resources)
        {
            if(resourceDescriptor.getName().endsWith("." + supportedExtension))
            {
                String resourceUrl = ResourceUrlFormatter.getResourceUrl("/", completeKey, resourceDescriptor.getName());
                RequestDispatcher rd = request.getRequestDispatcher(resourceUrl);
                try
                {
                    rd.include(request, response);
                }
                catch (ServletException e)
                {
                    throw new DownloadException(e);
                }
                catch (IOException e)
                {
                    throw new DownloadException(e);
                }
            }
        }
    }
}
