package com.atlassian.plugin.resourcedownload;

import java.io.Writer;

public interface PluginWebResourceManager
{
    /**
     * Indicates to that a given plugin web resource is required. All resources called via this method must be
     * included when {@link #writeRequiredResources(Writer)} is called.
     *
     * @param resourceName The fully qualified plugin web resource name (eg <code>jira.webresources:scriptaculous</code>)
     * @see #writeRequiredResources(Writer)
     */
    public void requireResource(String resourceName);

    /**
     * Writes out the resource tags to the previously required resources called via {@link #requireResource(String)}.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     */
    public void writeRequiredResources(Writer writer);

    public String getRequiredResources();

    /**
     * Writes the resource tags of the specified resource to the writer.
     * If you need it as a String to embed the tags in a template, use {@link #getRequiredResources()}.
     */
    public void writeResourceTags(String resourceName, Writer writer);

    public String getResourceTags(String resourceName);
}
