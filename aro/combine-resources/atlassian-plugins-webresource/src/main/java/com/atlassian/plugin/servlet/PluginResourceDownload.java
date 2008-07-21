package com.atlassian.plugin.servlet;

import com.atlassian.plugin.*;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

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

    public void serveFile(HttpServletRequest request, HttpServletResponse response, ApplicationDownloadContext context) throws IOException
    {
        String requestUri = URLDecoder.decode(request.getRequestURI(), context.getCharacterEncoding());
        PluginResource resource = urlParser.parse(requestUri);

        if (resource != null)
        {
            servePluginResource(resource, request, response, context);
        }
        else
        {
            log.info("Invalid resource path spec: " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void servePluginResource(PluginResource pluginResource, HttpServletRequest request,
                                       HttpServletResponse response, ApplicationDownloadContext context) throws IOException
    {
        String moduleCompleteKey = pluginResource.getModuleCompleteKey();
        String resourceName = pluginResource.getResourceName();
        DownloadableResource resource = null;

        // resource from the module
        if (moduleCompleteKey.indexOf(":") > -1)
        {
            ModuleDescriptor moduleDescriptor = pluginAccessor.getPluginModule(moduleCompleteKey);
            if (moduleDescriptor != null && pluginAccessor.isPluginModuleEnabled(moduleCompleteKey))
            {
                resource = getResourceFromModule(moduleDescriptor, resourceName, "", context);
            }
            else
            {
                log.info("Module not found: " + moduleCompleteKey);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        else // resource from plugin
        {
            Plugin plugin = pluginAccessor.getPlugin(moduleCompleteKey);
            resource = getResourceFromPlugin(plugin, resourceName, "", context);
        }

        if (resource == null)
            resource = getResourceFromPlugin(getPlugin(moduleCompleteKey), resourceName, "", context);

        if (resource == null)
        {
            log.info("Unable to find resource for plugin: " + moduleCompleteKey + " and path: " + resourceName);
            return;
        }

        resource.serveResource(request, response);
    }

    private Plugin getPlugin(String moduleKey)
    {
        if (moduleKey.indexOf(':') < 0 || moduleKey.indexOf(':') == moduleKey.length() - 1)
            return null;

        return pluginAccessor.getPlugin(moduleKey.substring(0, moduleKey.indexOf(':')));
    }

    private DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String resourcePath,
                                                       String filePath, ApplicationDownloadContext context)
    {
        Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);

        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, filePath, context);
        }

        String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
            return null;

        return getResourceFromModule(moduleDescriptor, nextParts[0], nextParts[1] + filePath, context);
    }

    private DownloadableResource getResourceFromPlugin(Plugin plugin, String resourcePath,
                                                       String filePath, ApplicationDownloadContext context)
    {
        if(plugin == null)
            return null;

        ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);
        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, filePath, context);
        }

        String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
            return null;

        return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath, context);
    }

    private DownloadableResource getDownloadablePluginResource(Plugin plugin, ResourceLocation resourceLocation,
                                                               String filePath, ApplicationDownloadContext context)
    {
        // this allows plugins that are loaded from the web to be served
        if ("webContext".equalsIgnoreCase(resourceLocation.getParameter("source")))
            return new DownloadableWebResource(plugin, resourceLocation, filePath, context);

        return new DownloadableClasspathResource(plugin, resourceLocation, filePath, context);
    }

    String[] splitLastPathPart(String resourcePath)
    {
        int indexOfSlash = resourcePath.lastIndexOf('/');
        if (resourcePath.endsWith("/")) // skip over the trailing slash
        {
            indexOfSlash = resourcePath.lastIndexOf('/', indexOfSlash - 1);
        }

        if (indexOfSlash < 0) return null;

        return new String[] {
            resourcePath.substring(0, indexOfSlash + 1),
            resourcePath.substring(indexOfSlash + 1)
        };
    }

    /**
     * @deprecated Since 2.0
     */
    public void serveFile(BaseFileServerServlet s, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String requestUri = s.urlDecode(request.getRequestURI());
        PluginResource resource = urlParser.parse(requestUri);

        if (resource != null)
        {
            servePluginResource(resource, request, response, new LegacyDownloadContext(s));
        }
        else
        {
            log.info("Invalid resource path spec: " + request.getRequestURI());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}

