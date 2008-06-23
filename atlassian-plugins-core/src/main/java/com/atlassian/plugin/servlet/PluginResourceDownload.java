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
 * <p/>
 * The URL that it parses looks like this: <br> <code>{server root}/download/resources/{plugin key}:{module
 * key}/{resource name}</code>
 */
public class PluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluginResourceDownload.class);
    private static final String DOWNLOAD_RESOURCE = "download";
    private final ResourceUrlParser urlParser = new ResourceUrlParser(BaseFileServerServlet.RESOURCE_URL_PREFIX);
    private PluginAccessor pluginAccessor;

    // no arg constructor for confluence
    public PluginResourceDownload()
    {
    }

    public PluginResourceDownload(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(String urlPath)
    {
        return urlParser.matches(urlPath);
    }

    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginAccessor = pluginManager;
    }

    public void serveFile(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws IOException
    {
        String requestUri = servlet.urlDecode(httpServletRequest.getRequestURI());
        PluginResource resource = urlParser.parse(requestUri);

        if (resource != null)
        {
            servePluginResource(servlet, httpServletRequest, httpServletResponse, resource.getModuleCompleteKey(),
                resource.getResourceName());
        }
        else
        {
            log.info("Invalid resource path spec: " + httpServletRequest.getRequestURI());
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void servePluginResource(BaseFileServerServlet servlet, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse, String moduleCompleteKey, String resourceName)
        throws IOException
    {
        DownloadableResource resource = null;

        // resource from the module
        if (moduleCompleteKey.indexOf(":") > -1)
        {
            ModuleDescriptor moduleDescriptor = pluginAccessor.getPluginModule(moduleCompleteKey);
            if (moduleDescriptor != null && pluginAccessor.isPluginModuleEnabled(moduleCompleteKey))
            {
                resource = getResourceFromModule(moduleDescriptor, resourceName, servlet);
            }
            else
            {
                log.info("Module not found: " + moduleCompleteKey);
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        else // resource from plugin
        {
            Plugin plugin = pluginAccessor.getPlugin(moduleCompleteKey);
            resource = getResourceFromPlugin(plugin, resourceName, "", servlet);
        }

        if (resource == null)
        {
            resource = getResourceFromPlugin(moduleCompleteKey, resourceName, servlet);
        }

        if (resource != null)
        {
            resource.serveResource(httpServletRequest, httpServletResponse);
        }
        else
        {
            log.info("Unable to find resource for plugin: " + moduleCompleteKey + " and path: " + resourceName);
        }
    }

    private DownloadableResource getResourceFromPlugin(String moduleKey, String resourcePath,
        BaseFileServerServlet servlet)
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

        return getResourceFromPlugin(plugin, resourcePath, "", servlet);
    }

    private DownloadableResource getResourceFromPlugin(Plugin plugin, String resourcePath, String filePath,
        BaseFileServerServlet servlet)
    {
        ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);

        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(servlet, plugin, resourceLocation, filePath);
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

    private DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String filePath,
        BaseFileServerServlet servlet)
    {
        return getResourceFromModule(moduleDescriptor, filePath, "", servlet);
    }

    DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String resourcePath, String filePath,
        BaseFileServerServlet servlet)
    {
        Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);

        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(servlet, plugin, resourceLocation, filePath);
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

    private DownloadableResource getDownloadablePluginResource(BaseFileServerServlet servlet, Plugin plugin,
        ResourceLocation resourceLocation, String filePath)
    {
        if ("webContext".equalsIgnoreCase(resourceLocation.getParameter(
            "source")))    // this allows plugins that are loaded from the web to be served
            return new DownloadableWebResource(servlet, plugin, resourceLocation, filePath);
        else
            return new DownloadableClasspathResource(servlet, plugin, resourceLocation, filePath);
    }

    String[] splitLastPathPart(String resourcePath)
    {
        int indexOfSlash = resourcePath.lastIndexOf('/');
        if (resourcePath.endsWith("/")) // skip over the trailing slash
        {
            indexOfSlash = resourcePath.lastIndexOf('/', indexOfSlash - 1);
        }

        if (indexOfSlash < 0) return null;

        return new String[]{
            resourcePath.substring(0, indexOfSlash + 1),
            resourcePath.substring(indexOfSlash + 1)
        };
    }
}

