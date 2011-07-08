package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.webresource.PluginResourceLocatorImpl.RESOURCE_SOURCE_PARAM;
import static com.atlassian.plugin.webresource.SinglePluginResource.URL_PREFIX;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableClasspathResource;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.DownloadableWebResource;
import com.atlassian.plugin.servlet.ForwardableResource;
import com.atlassian.plugin.servlet.ServletContextFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleDownloadableResourceBuilder implements DownloadableResourceBuilder, DownloadableResourceFinder
{
    private static final String DOWNLOAD_TYPE = "download";
    private static final Logger log = LoggerFactory.getLogger(SingleDownloadableResourceBuilder.class);

    private final PluginAccessor pluginAccessor;
    final private ServletContextFactory servletContextFactory;

    public SingleDownloadableResourceBuilder(final PluginAccessor pluginAccessor, final ServletContextFactory servletContextFactory)
    {
        this.pluginAccessor = pluginAccessor;
        this.servletContextFactory = servletContextFactory;
    }

    public boolean matches(final String path)
    {
        return path.indexOf(URL_PREFIX) != -1;
    }

    public DownloadableResource parse(final String path, final Map<String, String> params) throws UrlParseException
    {
        final int indexOfPrefix = path.indexOf(URL_PREFIX);
        String libraryAndResource = path.substring(indexOfPrefix + URL_PREFIX.length() + 1);

        if (libraryAndResource.indexOf('?') != -1) // remove query parameters
        {
            libraryAndResource = libraryAndResource.substring(0, libraryAndResource.indexOf('?'));
        }

        final String[] parts = libraryAndResource.split("/", 2);

        if (parts.length != 2)
        {
            throw new UrlParseException("Could not parse invalid plugin resource url: " + path);
        }

        final PluginResource resource = new SinglePluginResource(parts[1], parts[0], path.substring(0, indexOfPrefix).length() > 0);

        return find(resource.getModuleCompleteKey(), resource.getResourceName());
    }

    public DownloadableResource find(final String moduleKey, final String resourceName)
    {
        return locatePluginResource(moduleKey, resourceName);
    }

    private DownloadableResource locatePluginResource(final String moduleCompleteKey, final String resourceName)
    {
        DownloadableResource resource;

        // resource from the module
        if (moduleCompleteKey.indexOf(":") > -1)
        {
            final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
            if (moduleDescriptor != null)
            {
                resource = getResourceFromModule(moduleDescriptor, resourceName, "");
            }
            else
            {
                log.info("Module not found: " + moduleCompleteKey);
                return null;
            }
        }
        else
        // resource from plugin
        {
            resource = getResourceFromPlugin(pluginAccessor.getPlugin(moduleCompleteKey), resourceName, "");
        }

        if (resource == null)
        {
            resource = getResourceFromPlugin(getPlugin(moduleCompleteKey), resourceName, "");
        }

        if (resource == null)
        {
            log.info("Unable to find resource for plugin: " + moduleCompleteKey + " and path: " + resourceName);
            return null;
        }

        return resource;
    }

    private Plugin getPlugin(final String moduleKey)
    {
        final int semicolonIndex = moduleKey.indexOf(':');
        if ((semicolonIndex < 0) || (semicolonIndex == moduleKey.length() - 1))
        {
            return null;
        }

        return pluginAccessor.getPlugin(moduleKey.substring(0, semicolonIndex));
    }

    private DownloadableResource getResourceFromModule(final ModuleDescriptor<?> moduleDescriptor, final String resourcePath, final String filePath)
    {
        final Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        final ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_TYPE, resourcePath);

        if (resourceLocation != null)
        {
            boolean disableMinification = false;
            // I think it should always be a WebResourceModuleDescriptor, but
            // not sure...
            if (moduleDescriptor instanceof WebResourceModuleDescriptor)
            {
                disableMinification = ((WebResourceModuleDescriptor) moduleDescriptor).isDisableMinification();
            }
            return getDownloadablePluginResource(plugin, resourceLocation, moduleDescriptor, filePath, disableMinification);
        }

        final String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
        {
            return null;
        }

        return getResourceFromModule(moduleDescriptor, nextParts[0], nextParts[1] + filePath);
    }

    private DownloadableResource getResourceFromPlugin(final Plugin plugin, final String resourcePath, final String filePath)
    {
        if (plugin == null)
        {
            return null;
        }

        final ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_TYPE, resourcePath);
        if (resourceLocation != null)
        {
            return getDownloadablePluginResource(plugin, resourceLocation, null, filePath, false);
        }

        final String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null)
        {
            return null;
        }

        return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath);
    }

    private DownloadableResource getDownloadablePluginResource(final Plugin plugin, final ResourceLocation resourceLocation, final ModuleDescriptor<?> descriptor, final String filePath, final boolean disableMinification)
    {
        final String sourceParam = resourceLocation.getParameter(RESOURCE_SOURCE_PARAM);

        // serve by forwarding the request to the location - batching not
        // supported
        if ("webContext".equalsIgnoreCase(sourceParam))
        {
            return new ForwardableResource(resourceLocation);
        }

        DownloadableResource actualResource;
        // serve static resources from the web application - batching supported
        if ("webContextStatic".equalsIgnoreCase(sourceParam))
        {
            actualResource = new DownloadableWebResource(plugin, resourceLocation, filePath, servletContextFactory.getServletContext(),
                disableMinification);
        }
        else
        {
            actualResource = new DownloadableClasspathResource(plugin, resourceLocation, filePath);
        }

        DownloadableResource result = actualResource;
        // web resources are able to be transformed during delivery
        if (descriptor instanceof WebResourceModuleDescriptor)
        {
            DownloadableResource lastResource = actualResource;
            final WebResourceModuleDescriptor desc = (WebResourceModuleDescriptor) descriptor;
            for (final WebResourceTransformation list : desc.getTransformations())
            {
                if (list.matches(resourceLocation))
                {
                    lastResource = list.transformDownloadableResource(pluginAccessor, actualResource, resourceLocation, filePath);
                }
            }
            result = lastResource;
        }
        return result;
    }

    // pacakge protected so we can test it
    String[] splitLastPathPart(final String resourcePath)
    {
        int indexOfSlash = resourcePath.lastIndexOf('/');
        if (resourcePath.endsWith("/")) // skip over the trailing slash
        {
            indexOfSlash = resourcePath.lastIndexOf('/', indexOfSlash - 1);
        }

        if (indexOfSlash < 0)
        {
            return null;
        }

        return new String[] { resourcePath.substring(0, indexOfSlash + 1), resourcePath.substring(indexOfSlash + 1) };
    }
}
