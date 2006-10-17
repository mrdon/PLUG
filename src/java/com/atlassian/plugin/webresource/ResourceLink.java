package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;

/**
 * A way of providing links to resources.  These are may be application specific, or not.
 *
 * @see com.atlassian.plugin.webresource.AbstractStaticResourceLink
 * @see com.atlassian.plugin.webresource.PluginResourceLink
 * @see 
 */
public interface ResourceLink
{
    /**
     * Return a link to the download the resource.  It should not include the context path.  It should begin with a '/'.
     * An example might be:
     * <pre>
     * <code>/download/static/{build num}/{plugin version}/{system date}/pluginKey/{plugin key}:{module key}/{resource name}</code>
     * </pre>
     * Or
     * <pre>
     * <code>/download/resources/{plugin key}:{module key}/{resource name}</code>
     * </pre>
     */
    public String getLinkToResource(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor);
}
