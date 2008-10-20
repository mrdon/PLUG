package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.List;
import java.util.Map;

public interface PluginResourceLocator
{
    boolean matches(String url);

    DownloadableResource getDownloadableResource(String url, Map<String, String> queryParams);

    List<PluginResource> getPluginResource(String moduleCompleteKey);

    public String getStaticResourceUrlPrefix();

    public String getStaticResourceUrlPrefix(String resourceCounter);

    public String getStaticResourceUrl(String pluginModuleKey, String resourceName);
}