package com.atlassian.plugin.resourcedownload;

import com.atlassian.plugin.resourcedownload.servlet.DownloadableResource;

import java.util.List;

public interface PluginResourceLocator
{
    boolean matches(String url);

    DownloadableResource locateByUrl(String url);

    List<Resource> locateByCompleteKey(String moduleCompleteKey);

    /**
     * @param pluginModuleKey complete plugin module key
     * @return returns the url of this plugin resource
     */
    //public String getStaticPluginResourceUrl(String pluginModuleKey, String resourceName);
}
