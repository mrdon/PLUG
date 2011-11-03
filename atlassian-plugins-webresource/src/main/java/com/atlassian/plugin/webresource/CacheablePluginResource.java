package com.atlassian.plugin.webresource;

/**
 * Represents a plugin resource that will construct cache friendly URLS.
 * @since 2.11.0
 */
public interface CacheablePluginResource extends PluginResource
{
    /**
     * Return a cache friendly url. 
     * @param integration WebResourceIntegration to resolve versions and locales to enable building of
     * cachable urls.
     * @return the url for this plugin resource.
     */
    String getCacheUrl(WebResourceIntegration integration);
}
