package com.atlassian.plugin.servlet;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.util.LastModifiedHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: detkin
 * Date: Jan 20, 2006
 * Time: 12:00:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class PluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluginResourceDownload.class);
    private PluginManager pluginManager;

    // no arg constructor for confluence
    public PluginResourceDownload(){}

    public PluginResourceDownload(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    private static class DownloadableResource
    {
        private ResourceLocation resourceDescriptor;
        private String extraPath;
        private String pluginKey;
        private BaseFileServerServlet servlet;

        public DownloadableResource(BaseFileServerServlet servlet, String pluginKey, ResourceLocation resourceDescriptor, String extraPath)
        {
            if (extraPath != null && !"".equals(extraPath.trim()) && !resourceDescriptor.getLocation().endsWith("/"))
            {
                extraPath = "/" + extraPath;
            }

            this.resourceDescriptor = resourceDescriptor;
            this.extraPath = extraPath;
            this.pluginKey = pluginKey;
            this.servlet = servlet;
        }

        public String getContentType()
        {
            if (resourceDescriptor.getContentType() == null)
            {
                return servlet.getContentType(getLocation());
            }

            return resourceDescriptor.getContentType();
        }

        public String getLocation()
        {
            return resourceDescriptor.getLocation() + extraPath;
        }

        public String toString()
        {
            return "Resource: " + getLocation() + " (" + getContentType() + ")";
        }

        public String getPluginKey()
        {
            return pluginKey;
        }
    }

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX) != -1;
    }

    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public void serveFile(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
    {
        String[] parts = splitIntoLibraryAndResource(httpServletRequest.getRequestURI(), servlet);
        if (parts.length == 2)
        {
            servePluginResource(servlet, httpServletRequest, httpServletResponse, parts[0], parts[1]);
        }
        else
        {
            log.info("Invalid resource path spec: " + httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void servePluginResource(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String moduleKey, String filePath)
            throws IOException
    {
        ModuleDescriptor moduleDescriptor = pluginManager.getPluginModule(moduleKey);
        if (moduleDescriptor != null && pluginManager.isPluginModuleEnabled(moduleKey))
        {
            DownloadableResource resource = getResourceFromModule(moduleDescriptor, filePath, servlet);

            if (resource == null)
            {
                resource = getResourceFromPlugin(moduleKey, filePath, servlet);
            }

            if (resource != null)
            {
                if (!checkResourceNotModified(resource, httpServletRequest, httpServletResponse))
                    serveDownloadableResource(servlet, httpServletResponse, resource);
            }
            else
            {
                log.info("Unable to find resource for module: " + moduleKey + " and path: " + filePath);
            }
        }
        else
        {
            log.info("Module not found: " + moduleKey);
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
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
    private boolean checkResourceNotModified(DownloadableResource resource, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        Plugin plugin = pluginManager.getPlugin(resource.getPluginKey());
        Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }

    private DownloadableResource getResourceFromPlugin(String moduleKey, String filePath, BaseFileServerServlet servlet)
    {
        if (moduleKey.indexOf(':') < 0 || moduleKey.indexOf(':') == moduleKey.length() - 1)
        {
            return null;
        }

        Plugin plugin = pluginManager.getPlugin(moduleKey.substring(0, moduleKey.indexOf(':')));
        if (plugin == null)
        {
            return null;
        }

        return getResourceFromPlugin(plugin, filePath, "", servlet);
    }

    private DownloadableResource getResourceFromPlugin(Plugin plugin, String resourcePath, String filePath, BaseFileServerServlet servlet)
    {
        ResourceLocation rd = plugin.getResourceLocation("download", resourcePath);

        if (rd != null)
        {
            return new DownloadableResource(servlet, plugin.getKey(), rd, filePath);
        }
        else
        {
            String[] nextParts = splitLastPathPart(resourcePath);
            if (nextParts == null)
            {
                return null;
            }
            else
            {
                return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath, servlet);
            }
        }
    }

    private DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String filePath, BaseFileServerServlet servlet)
    {
        return getResourceFromModule(moduleDescriptor, filePath, "", servlet);
    }

    private DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String resourcePath, String filePath, BaseFileServerServlet servlet)
    {
        ResourceLocation rd = moduleDescriptor.getResourceLocation("download", resourcePath);

        if (rd != null)
        {
            return new DownloadableResource(servlet, moduleDescriptor.getPluginKey(), rd, filePath);
        }
        else
        {
            String[] nextParts = splitLastPathPart(resourcePath);
            if (nextParts == null)
            {
                return null;
            }
            else
            {
                return getResourceFromModule(moduleDescriptor, nextParts[0], nextParts[1] + filePath, servlet);
            }
        }
    }

    private String[] splitLastPathPart(String resourcePath)
    {
        String slash = "";
        while (resourcePath.endsWith("/"))
        {
            slash = "/";
            resourcePath = resourcePath.substring(0, resourcePath.length() - 1);
        }

        int i = resourcePath.lastIndexOf('/');

        if (i < 0)
        {
            return null;
        }

        return new String[]{
                resourcePath.substring(0, i + 1),
                resourcePath.substring(i + 1) + slash
        };
    }

    private void serveDownloadableResource(BaseFileServerServlet servlet, HttpServletResponse httpServletResponse, DownloadableResource resource) throws IOException
    {
        log.debug("Serving: " + resource);
        InputStream resourceStream = pluginManager.getPluginResourceAsStream(resource.getPluginKey(), resource.getLocation());
        if (resourceStream != null)
        {
            httpServletResponse.setContentType(resource.getContentType());
            servlet.serveFileImpl(httpServletResponse, resourceStream);

            try
            {
                resourceStream.close();
            }
            catch (IOException e)
            {
                log.error("Could not close input stream on resource:", e);
            }

        }
        else
        {
            log.info("Resource not found: " + resource);
        }
    }


    private String[] splitIntoLibraryAndResource(String requestUri, BaseFileServerServlet servlet)
    {
        requestUri = servlet.urlDecode(requestUri);
        int afterTheResourcesString = requestUri.indexOf(BaseFileServerServlet.RESOURCE_URL_PREFIX);
        requestUri = requestUri.substring(afterTheResourcesString + BaseFileServerServlet.RESOURCE_URL_PREFIX.length() + 1);
        return requestUri.split("/", 2);
    }

}

