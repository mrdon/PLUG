package com.atlassian.plugin.webresource.batch;

import com.atlassian.plugin.webresource.PluginResource;
import com.atlassian.plugin.webresource.batch.BatchResource;

import java.util.List;

public interface Batched
{
    public List<PluginResource> getPluginResources(BatchResource batchResource);

    public List<BatchResource> getBatchResources();
}
