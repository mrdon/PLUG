package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.Map;

/**
 * Constructs a plugin resource from a given url
 */
interface DownloadableResourceBuilder
{
    /**
     * Returns true if the builder can parse the given url
     * @param path - the url to parse
     * @return true if the builder can parse the given url
     */
    public boolean matches(String path);

    /**
     * Parses the url and params and then builds a resource to upload
     * @param path - the url
     * @param params - query parameters provided in the url
     * @return a resource to upload
     * @throws UrlParseException - if the path cannot be parsed
     */
    public DownloadableResource parse(String path, Map<String, String> params) throws UrlParseException;
}
