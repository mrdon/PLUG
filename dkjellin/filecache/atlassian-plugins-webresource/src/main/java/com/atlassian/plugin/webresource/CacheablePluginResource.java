package com.atlassian.plugin.webresource;

public interface CacheablePluginResource extends PluginResource
{
    /**
     * @param integration WebResourceIntegration to resolve versions and locales to enable building of
     * cachable urls.
     * @return the url for this plugin resource.
     */
    String getCacheUrl(WebResourceIntegration integration);
}
