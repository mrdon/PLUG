package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import com.atlassian.plugin.elements.ResourceDescriptor;

public class PluginResourceLink implements ResourceLink
{
    /**
     * Return a resource link of the form:
     * <pre>
     * <code>/download/resources/{plugin key}:{module key}/{resource name}</code>
     * </pre>
     */
    public String getLinkToResource(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor)
    {
        return "/" + BaseFileServerServlet.SERVLET_PATH + "/" + BaseFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleDescriptor.getCompleteKey() + "/" + resourceDescriptor.getName();
    }
}
