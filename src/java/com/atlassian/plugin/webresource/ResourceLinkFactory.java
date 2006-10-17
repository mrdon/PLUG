package com.atlassian.plugin.webresource;

import com.atlassian.plugin.elements.ResourceDescriptor;

/**
 * A way of getting resource links.  Implementations of this class are application specific.
 */
public interface ResourceLinkFactory
{
    public ResourceLink getResourceLink(ResourceDescriptor resourceDescriptor);
}
