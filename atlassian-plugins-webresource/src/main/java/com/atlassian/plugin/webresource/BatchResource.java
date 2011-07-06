package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;

import java.util.Map;

/**
 * Interface for plugin resources that serve batches.
 */
public interface BatchResource
{
    String getType();

    Map<String, String> getParams();
}
