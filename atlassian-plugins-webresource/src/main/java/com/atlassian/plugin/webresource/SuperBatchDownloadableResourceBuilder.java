package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.util.EfficientStringUtils.endsWith;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.DEFAULT_RESOURCE_NAME_PREFIX;
import static com.atlassian.plugin.webresource.SuperBatchPluginResource.URL_PREFIX;

public class SuperBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(SuperBatchDownloadableResourceBuilder.class);
    private final ResourceDependencyResolver dependencyResolver;

    public SuperBatchDownloadableResourceBuilder(ResourceDependencyResolver dependencyResolver, PluginAccessor pluginAccessor,
                                                 WebResourceUrlProvider webResourceUrlProvider, DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
        this.dependencyResolver = dependencyResolver;
    }

    public boolean matches(String path)
    {
        String type = ResourceUtils.getType(path);
        return path.indexOf(URL_PREFIX) != -1 && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);

    }

    public SuperBatchPluginResource parse(String path, Map<String, String> params)
    {
        String type = ResourceUtils.getType(path);

        List<DownloadableResource> resources = new ArrayList<DownloadableResource>();
        for (final String moduleKey : dependencyResolver.getSuperBatchDependencies())
        {
            resources.addAll(resolve(moduleKey, type, params));
        }

        SuperBatchPluginResource batchResource = new SuperBatchPluginResource(type, params, resources);

        if (log.isDebugEnabled())
        {
            log.debug(batchResource.toString());
        }

        return batchResource;
    }
}
