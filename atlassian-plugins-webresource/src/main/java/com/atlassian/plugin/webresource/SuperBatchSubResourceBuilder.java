package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.Arrays;
import java.util.Map;

import static com.atlassian.plugin.webresource.SuperBatchPluginResource.URL_PREFIX;

/**
 * Provides a fallback to serve resources relative to a super batch resource
 * In practice, the resources url should be transformed via the
 * {{com.atlassian.plugin.webresource.RelativeURLTransformResource}}.
 * This builder is in place in case this does not happen
 * @since 2.9.0
 */
public class SuperBatchSubResourceBuilder implements DownloadableResourceBuilder
{
    private final ResourceDependencyResolver dependencyResolver;
    private final DownloadableResourceFinder resourceFinder;

    public SuperBatchSubResourceBuilder(ResourceDependencyResolver dependencyResolver, DownloadableResourceFinder resourceFinder)
    {
        this.dependencyResolver = dependencyResolver;
        this.resourceFinder = resourceFinder;
    }

    public boolean matches(String path)
    {
        return path.contains(URL_PREFIX);
    }

    public DownloadableResource parse(String path, Map<String, String> params) throws UrlParseException
    {
        String type = ResourceUtils.getType(path);
        String resourceName = getResourceName(path);

        for (WebResourceModuleDescriptor moduleDescriptor : dependencyResolver.getSuperBatchDependencies())
        {
            DownloadableResource resource = resourceFinder.find(moduleDescriptor.getCompleteKey(), resourceName);

            if (resource != null)
            {
                return new BatchSubResource(resourceName, type, params, Arrays.asList(resource));
            }
        }

        return new BatchSubResource(resourceName, type, params);
    }

    private String getResourceName(String path)
    {
        int startIndex = path.indexOf(URL_PREFIX) + URL_PREFIX.length();
        return path.substring(startIndex);
    }
}
