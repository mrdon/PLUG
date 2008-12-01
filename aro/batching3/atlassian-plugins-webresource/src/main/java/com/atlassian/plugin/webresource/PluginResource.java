package com.atlassian.plugin.webresource;

import java.util.Map;

/**
 * Represents a plugin resource.  
 */
public interface PluginResource
{
    /**
     * Returns true if caching for this resource is supported. Use this check to append a static
     * caching url prefix to this resource's url.
     */
    boolean isCacheSupported();

    /**
     * Returns the url for this plugin resource.
     */
    String getUrl();

    /**
     * Returns the resource name for the plugin resource.
     */
    String getResourceName();

    /**
     * Returns the plugin module's complete key for which this resource belongs to.
     */
    String getModuleCompleteKey();

    /**
     * Returns a map of parameter key and value pairs for this resource.
     */
    Map<String, String> getParams();
}