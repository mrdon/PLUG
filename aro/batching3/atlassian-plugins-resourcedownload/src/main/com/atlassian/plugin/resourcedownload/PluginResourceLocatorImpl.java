package com.atlassian.plugin.resourcedownload;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import com.atlassian.plugin.resourcedownload.servlet.ServletContextFactory;
import com.atlassian.plugin.resourcedownload.servlet.DownloadableResource;
import com.atlassian.plugin.resourcedownload.servlet.ClasspathResource;
import com.atlassian.plugin.resourcedownload.servlet.WebappResource;

public class PluginResourceLocatorImpl implements PluginResourceLocator
{
    private static final Log log = LogFactory.getLog(PluginWebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    private static final String DOWNLOAD_TYPE = "download";

    private static String[] BATCH_PARAMS = new String[] { "ieonly", "media" };

    private PluginAccessor pluginAccessor;
    private WebResourceIntegration webResourceIntegration;
    private ContentTypeResolver contentTypeResolver;
    private ServletContextFactory servletContextFactory;

    public PluginResourceLocatorImpl(PluginAccessor pluginAccessor, WebResourceIntegration webResourceIntegration,
        ContentTypeResolver contentTypeResolver, ServletContextFactory servletContextFactory)
    {
        this.pluginAccessor = pluginAccessor;
        this.webResourceIntegration = webResourceIntegration;
        this.contentTypeResolver = contentTypeResolver;
        this.servletContextFactory = servletContextFactory;
    }

    public boolean matches(String url)
    {
        return url.indexOf(PluginResource.URL_PREFIX) != -1 || url.indexOf(BatchResource.URL_PREFIX) != -1;
    }

    public DownloadableResource locateByUrl(String url)
    {
        if(url.indexOf(PluginResource.URL_PREFIX) != -1)
            return locatePluginResource(url);

        if(url.indexOf(BatchResource.URL_PREFIX) != -1)
            return locateBatchResource(url);

        log.error("Cannot locate resource for unknown url: " + url);
        return null;
    }

    private DownloadableResource locateBatchResource(String url)
    {
        BatchResource batchResource = BatchResource.parse(url);
        ModuleDescriptor moduleDescriptor = pluginAccessor.getEnabledPluginModule(batchResource.getModuleCompleteKey());
        for(ResourceDescriptor resourceDescriptor : moduleDescriptor.getResourceDescriptors(DOWNLOAD_TYPE))
        {
            if(isResourceInBatch(resourceDescriptor, batchResource))
                batchResource.add(locatePluginResource(moduleDescriptor.getCompleteKey(), resourceDescriptor.getName()));
        }
        return batchResource;
    }

    private boolean isResourceInBatch(ResourceDescriptor resourceDescriptor, BatchResource batchResource)
    {
        if(!resourceDescriptor.getName().endsWith("." + batchResource.getType()))
            return false;

        for(Map.Entry<String, String> entry : batchResource.getParams().entrySet())
        {
            if(!entry.getValue().equals(resourceDescriptor.getParameter(entry.getKey())))
                return false;
        }
        return true;
    }

    private DownloadableResource locatePluginResource(String url)
    {
        PluginResource resource = PluginResource.parse(url);
        return locatePluginResource(resource.getModuleCompleteKey(), resource.getResourceName());
    }

    private DownloadableResource locatePluginResource(String moduleCompleteKey, String resourceName)
    {
        DownloadableResource resource;

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
                return null;
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
            return null;
        }

        return resource;
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
        ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_TYPE, resourcePath);

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

        ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_TYPE, resourcePath);
        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, filePath);
        }

        String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
            return null;

        return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath);
    }

    private String[] splitLastPathPart(String resourcePath)
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

    private DownloadableResource getDownloadablePluginResource(Plugin plugin, ResourceLocation resourceLocation, String filePath)
    {
        // this allows plugins that are loaded from the web to be served
        if ("webContext".equalsIgnoreCase(resourceLocation.getParameter("source")))
            return new WebappResource(plugin, resourceLocation, filePath, contentTypeResolver, servletContextFactory.getServletContext());

        return new ClasspathResource(plugin, resourceLocation, filePath, contentTypeResolver);
    }

    public List<Resource> locateByCompleteKey(String moduleCompleteKey)
    {
        ModuleDescriptor moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
        if (moduleDescriptor == null || !(moduleDescriptor instanceof WebResourceModuleDescriptor))
        {
            log.error("Error loading resource \"" + moduleDescriptor + "\". Resource is not a Web Resource Module");
            return Collections.EMPTY_LIST;
        }

        boolean singleMode = Boolean.valueOf(System.getProperty("plugin.webresources.single"));
        List<Resource> resources = new ArrayList<Resource>();

        for(ResourceDescriptor resourceDescriptor : moduleDescriptor.getResourceDescriptors())
        {
            String staticUrlPrefix = getStaticUrlPrefix((WebResourceModuleDescriptor) moduleDescriptor, resourceDescriptor);
            if (singleMode)
            {
                resources.add(new PluginResource(resourceDescriptor.getName(), moduleDescriptor.getCompleteKey(), staticUrlPrefix));
            }
            else
            {
                Resource batchResource = createBatchResource(moduleDescriptor.getCompleteKey(), resourceDescriptor, staticUrlPrefix);
                if(!resources.contains(batchResource))
                    resources.add(batchResource);
            }
        }
        return resources;
    }

    private Resource createBatchResource(String moduleCompleteKey, ResourceDescriptor resourceDescriptor, String staticUrlPrefix)
    {
        String name = resourceDescriptor.getName();
        String type = name.substring(name.lastIndexOf(".") + 1);
        Map<String, String> params = new TreeMap<String, String>();
        for(String param : BATCH_PARAMS) // todo get batch params depending on type
        {
            String value = resourceDescriptor.getParameter(param);
            if(StringUtils.isNotBlank(value))
                params.put(param, value);
        }

        return new BatchResource(moduleCompleteKey, type, params, staticUrlPrefix);
    }

    private String getStaticUrlPrefix(WebResourceModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor)
    {
        if (!"false".equalsIgnoreCase(resourceDescriptor.getParameter("cache"))) //todo preserve for batching
            return getStaticUrlPrefix(moduleDescriptor.getPlugin());

        return "";
    }

    private String getStaticUrlPrefix(Plugin plugin)
    {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        return webResourceIntegration.getBaseUrl() + "/" +
                STATIC_RESOURCE_PREFIX + "/" +
                webResourceIntegration.getSystemBuildNumber() + "/" +
                webResourceIntegration.getSystemCounter() + "/" +
                plugin.getPluginsVersion() + "/" +
                STATIC_RESOURCE_SUFFIX;
    }
}
