package com.atlassian.plugin.webresource;

import java.util.HashSet;
import java.util.Set;

/**
 * Container class for the state of a WebResourceManager
 */
public class WebResourceRequestContext
{
    private Set<String> resources;
    private Set<String> contexts;

    // constructor sets empty sets to ensure non-null values
    public WebResourceRequestContext()
    {
        this.resources = new HashSet<String>();
        this.contexts = new HashSet<String>();
    }

    public void setResources(Set<String> resources)
    {
        this.resources = resources;
    }

    public Set<String> getResources()
    {
        return resources;
    }

    public void setContexts(Set<String> contexts)
    {
        this.contexts = contexts;
    }

    public Set<String> getContexts()
    {
        return contexts;
    }
}
