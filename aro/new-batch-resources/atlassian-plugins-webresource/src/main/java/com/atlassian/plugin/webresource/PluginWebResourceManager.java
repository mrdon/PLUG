package com.atlassian.plugin.webresource;

import java.io.Writer;

public interface PluginWebResourceManager
{
    public void requireResource(String resourceName);

    public void writeRequiredResources(Writer writer);

    public void writeResourceContent(String resourceName, Writer writer);

    public void writeResourceTags(String resourceName, Writer writer);

    public String getRequiredResources();

    public String getResourceContent(String resourceName);

    public String getResourceTags(String resourceName);
}
