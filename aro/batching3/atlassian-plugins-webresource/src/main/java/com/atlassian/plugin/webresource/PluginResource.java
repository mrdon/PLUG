package com.atlassian.plugin.webresource;

import java.util.Map;

/**
 * Represents a plugin resource.  
 */
public interface PluginResource
{
    String getUrl();

    String getResourceName();

    String getModuleCompleteKey();

    Map<String, String> getParams();
}