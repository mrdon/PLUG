package com.atlassian.plugin.webresource;

import static com.atlassian.plugin.webresource.BatchPluginResource.URL_PREFIX;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @since 2.10
 */
class SingleBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(SingleBatchDownloadableResourceBuilder.class);

    public SingleBatchDownloadableResourceBuilder(final PluginAccessor pluginAccessor, final WebResourceUrlProvider webResourceUrlProvider, final DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceUrlProvider, resourceFinder);
    }

    public boolean matches(final String path)
    {
        return path.indexOf(URL_PREFIX) > -1;
    }

    public DownloadableResource parse(final String path, final Map<String, String> params) throws UrlParseException
    {
        final String type = ResourceUtils.getType(path);
        final int startIndex = path.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;
        final String typeAndModuleKey = path.substring(startIndex);
        final String[] parts = typeAndModuleKey.split("/", 2);

        if (parts.length < 2)
        {
            throw new UrlParseException("Could not parse invalid batch resource url: " + path);
        }
        final String moduleKey = parts[0];
        final String resourceName = parts[1];
        final BatchPluginResource batchResource = new BatchPluginResource(resourceName, moduleKey, type, params, resolve(moduleKey, type, params));

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
