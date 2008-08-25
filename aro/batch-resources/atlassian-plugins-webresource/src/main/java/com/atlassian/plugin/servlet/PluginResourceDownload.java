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
import java.net.URLDecoder;

/**
 * Supports the download of a single downloadable plugin resource, as described here:
 * http://confluence.atlassian.com/display/JIRA/Downloadable+plugin+resource
 * <p/>
 * The URL that it parses looks like this: <br> <code>{server root}/download/resources/{plugin key}:{module
 * key}/{resource name}</code>
 * <p/>
 * See {@link PluginResourcesDownload} classes for serving multiple plugin resource downloads.
 */
public class PluginResourceDownload implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluginResourceDownload.class);
    static final String DOWNLOAD_RESOURCE = "download";
    private final ResourceUrlParser urlParser = new ResourceUrlParser(AbstractFileServerServlet.RESOURCE_URL_PREFIX);
    private PluginAccessor pluginAccessor;
    private String characterEncoding = "UTF-8"; // default to sensible encoding
    private ContentTypeResolver contentTypeResolver;

    // no arg constructor for confluence
    public PluginResourceDownload()
    {
    }

    public PluginResourceDownload(String characterEncoding, PluginAccessor pluginAccessor, ContentTypeResolver contentTypeResolver)
    {
        this.pluginAccessor = pluginAccessor;
        this.contentTypeResolver = contentTypeResolver;
        this.characterEncoding = characterEncoding;
    }

    public boolean matches(String urlPath)
    {
        return urlParser.matches(urlPath);
    }

    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException
    {
        try
        {
            String requestUri = URLDecoder.decode(request.getRequestURI(), characterEncoding);
            PluginResource resource = urlParser.parse(requestUri);

            if (resource != null)
            {
                servePluginResource(resource, request, response);
            }
            else
            {
                log.info("Invalid resource path spec: " + request.getRequestURI());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        catch(IOException e)
        {
            throw new DownloadException(e);
        }
    }

    protected void servePluginResource(PluginResource pluginResource, HttpServletRequest request, HttpServletResponse response) throws IOException
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
                resource = getResourceFromModule(moduleDescriptor, resourceName, "");
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
            resource = getResourceFromPlugin(plugin, resourceName, "");
        }

        if (resource == null)
            resource = getResourceFromPlugin(getPlugin(moduleCompleteKey), resourceName, "");

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

    private DownloadableResource getResourceFromModule(ModuleDescriptor moduleDescriptor, String resourcePath, String filePath)
    {
        Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);

        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, filePath);
        }

        String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
            return null;

        return getResourceFromModule(moduleDescriptor, nextParts[0], nextParts[1] + filePath);
    }

    private DownloadableResource getResourceFromPlugin(Plugin plugin, String resourcePath, String filePath)
    {
        if (plugin == null)
            return null;

        ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_RESOURCE, resourcePath);
        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, filePath);
        }

        String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
            return null;

        return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath);
    }

    private DownloadableResource getDownloadablePluginResource(Plugin plugin, ResourceLocation resourceLocation, String filePath)
    {
        // this allows plugins that are loaded from the web to be served
        if ("webContext".equalsIgnoreCase(resourceLocation.getParameter("source")))
            return new DownloadableWebResource(plugin, resourceLocation, filePath, contentTypeResolver);

        return new DownloadableClasspathResource(plugin, resourceLocation, filePath, contentTypeResolver);
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

    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginAccessor = pluginManager;
    }

    public void setContentTypeResolver(ContentTypeResolver contentTypeResolver)
    {
        this.contentTypeResolver = contentTypeResolver;
    }

    /**
     * Sets the character enconding to use when decoding request urls.
     */
    public void setCharacterEncoding(String characterEncoding)
    {
        this.characterEncoding = characterEncoding;
    }
}

