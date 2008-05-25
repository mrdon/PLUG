package com.atlassian.plugin.servlet;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * A {@link SelectiveSelfServingModuleDescriptor} is amodule descriptor that MAY be able to serve up its odnw [@link DownloadableResource}
 * for a given resource name.  The Selective part comes from that fact that if its returns null,
 * the default processing should occur
 */
public interface SelectiveSelfServingModuleDescriptor extends ModuleDescriptor
{
    /**
     * Called to check with the {@link com.atlassian.plugin.ModuleDescriptor} if it can serve
     *
     * @param servlet      the BaseFileServerServlet in play
     * @param resourceName the name of the resource that may be served by the ModuleDescriptor
     * @return a DownloadableResource if it can server the reousrce of null if it cant, and default
     *         processing should occur.
     */
    DownloadableResource getDownloadableResource(BaseFileServerServlet servlet, String resourceName);
}
