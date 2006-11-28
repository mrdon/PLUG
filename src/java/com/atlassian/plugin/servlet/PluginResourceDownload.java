package com.atlassian.plugin.servlet;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A downloadable plugin resource, as described here: http://confluence.atlassian.com/display/JIRA/Downloadable+plugin+resource
 * <p>
 * The URL that it parses looks like this: <br>
 * <code>{server root}/download/resources/{plugin key}:{module key}/{resource name}</code>
 */
public class PluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluginResourceDownload.class);
    private PluginAccessor pluginAccessor;

    // no arg constructor for confluence
    public PluginResourceDownload(){}

    public PluginResourceDownload(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(String urlPath)
    {
        return urlPath.indexOf(BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX) != -1;
    }

    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginAccessor = pluginManager;
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

    protected void servePluginResource(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String key, String filePath)
            throws IOException
    {
       DownloadableResource resource = null;

        // resource from the module
        if (key.indexOf(":") > -1)
        {
            ModuleDescriptor moduleDescriptor = pluginAccessor.getPluginModule(key);
            if (moduleDescriptor != null && pluginAccessor.isPluginModuleEnabled(key))
            {
                resource = getResourceFromModule(moduleDescriptor, filePath, servlet);
            }
            else
            {
                log.info("Module not found: " + key);
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
       else // resource from plugin
        {
            Plugin plugin = pluginAccessor.getPlugin(key);
            resource = getResourceFromPlugin(plugin, filePath, "", servlet);
        }

        if (resource == null)
        {
            resource = getResourceFromPlugin(key, filePath, servlet);
        }

        if (resource != null)
        {
            resource.serveResource(httpServletRequest, httpServletResponse);
        }
        else
        {
            log.info("Unable to find resource for plugin: " + key + " and path: " + filePath);
        }
    }

    private DownloadableResource getResourceFromPlugin(String moduleKey, String filePath, BaseFileServerServlet servlet)
    {
        if (moduleKey.indexOf(':') < 0 || moduleKey.indexOf(':') == moduleKey.length() - 1)
        {
            return null;
        }

        Plugin plugin = pluginAccessor.getPlugin(moduleKey.substring(0, moduleKey.indexOf(':')));
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
            return getDownloadablePluginResource(servlet, plugin, rd, filePath);
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
        Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        ResourceLocation rd = moduleDescriptor.getResourceLocation("download", resourcePath);

        if (rd != null)
        {
            return getDownloadablePluginResource(servlet, plugin, rd, filePath);
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

    private DownloadableResource getDownloadablePluginResource(BaseFileServerServlet servlet, Plugin plugin, ResourceLocation rd, String filePath)
    {
        if ("webContext".equalsIgnoreCase(rd.getParameter("source")))    // this allows plugins that are loaded from the web to be served
            return new DownloadableWebResource(servlet, plugin, rd, filePath);
        else
            return new DownloadableClasspathResource(servlet, plugin, rd, filePath);
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

    private String[] splitIntoLibraryAndResource(String requestUri, BaseFileServerServlet servlet)
    {
        requestUri = servlet.urlDecode(requestUri);
        int afterTheResourcesString = requestUri.indexOf(BaseFileServerServlet.RESOURCE_URL_PREFIX);
        requestUri = requestUri.substring(afterTheResourcesString + BaseFileServerServlet.RESOURCE_URL_PREFIX.length() + 1);
        return requestUri.split("/", 2);
    }


}

