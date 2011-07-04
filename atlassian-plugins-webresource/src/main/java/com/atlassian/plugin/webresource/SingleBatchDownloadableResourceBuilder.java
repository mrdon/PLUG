package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.atlassian.plugin.webresource.BatchPluginResource.URL_PREFIX;

public class SingleBatchDownloadableResourceBuilder extends AbstractBatchResourceBuilder
{
    private static final Logger log = LoggerFactory.getLogger(SingleBatchDownloadableResourceBuilder.class);

    public SingleBatchDownloadableResourceBuilder(PluginAccessor pluginAccessor,
                                                  WebResourceIntegration webResourceIntegration,
                                                  DownloadableResourceFinder resourceFinder)
    {
        super(pluginAccessor, webResourceIntegration, resourceFinder);
    }

    public boolean matches(String path)
    {
        return path.indexOf(URL_PREFIX) > -1;
    }

    public DownloadableResource parse(String path, Map<String, String> params) throws UrlParseException
    {
        BatchPluginResource batchResource = getResource(path, params);

        if (log.isDebugEnabled())
        {
            log.debug(batchResource.toString());
        }

        addModuleToBatch(batchResource.getModuleCompleteKey(), batchResource);

        if (batchResource.isEmpty())
        {
            return getResourceFinder().find(batchResource.getModuleCompleteKey(), batchResource.getResourceName());
        }

        return batchResource;
    }

    /**
     * Parses the given url and query parameter map into a BatchPluginResource. Query paramters must be
     * passed in through the map, any in the url String will be ignored.
     * @param url         the url to parse
     * @param queryParams a map of String key and value pairs representing the query parameters in the url
     * @return the parsed BatchPluginResource
     * @throws UrlParseException if the url passed in is not a valid batch resource url
     */
    private static BatchPluginResource getResource(String url, final Map<String, String> queryParams) throws UrlParseException
    {
        final int startIndex = url.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;

        if (url.indexOf('?') != -1) // remove query parameters
        {
            url = url.substring(0, url.indexOf('?'));
        }

        final String typeAndModuleKey = url.substring(startIndex);
        final String[] parts = typeAndModuleKey.split("/", 2);

        if (parts.length < 2)
        {
            throw new UrlParseException("Could not parse invalid batch resource url: " + url);
        }

        final String moduleKey = parts[0];
        final String resourceName = parts[1];
        final String type = resourceName.substring(resourceName.lastIndexOf('.') + 1);

        return new BatchPluginResource(resourceName, moduleKey, type, queryParams);
    }

}
