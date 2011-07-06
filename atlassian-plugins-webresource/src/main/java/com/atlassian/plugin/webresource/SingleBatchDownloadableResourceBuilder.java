package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.webresource.BatchPluginResource.URL_PREFIX;

public class SingleBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(SingleBatchDownloadableResourceBuilder.class);

    public SingleBatchDownloadableResourceBuilder(PluginAccessor pluginAccessor,
                                                  WebResourceUrlProvider webResourceUrlProvider,
                                                  DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
    }

    public boolean matches(String path)
    {
        return path.indexOf(URL_PREFIX) > -1;
    }

    public DownloadableResource parse(String path, Map<String, String> params) throws UrlParseException
    {
        String type = ResourceUtils.getType(path);
        final int startIndex = path.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;
        final String typeAndModuleKey = path.substring(startIndex);
        final String[] parts = typeAndModuleKey.split("/", 2);

        if (parts.length < 2)
        {
            throw new UrlParseException("Could not parse invalid batch resource url: " + path);
        }
        final String moduleKey = parts[0];
        final String resourceName = parts[1];

        List<DownloadableResource> resources = new ArrayList<DownloadableResource>();
        resources.addAll(resolve(moduleKey, type, params));

        BatchPluginResource batchResource = new BatchPluginResource(resourceName, moduleKey, type, params, resources);

        if (log.isDebugEnabled())
        {
            log.debug(batchResource.toString());
        }

        if (batchResource.isEmpty())
        {
            return getResourceFinder().find(batchResource.getModuleCompleteKey(), batchResource.getResourceName());
        }

        return batchResource;
    }
}
