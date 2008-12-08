package com.atlassian.plugin.servlet.util;

import com.atlassian.plugin.hostcontainer.HostContainer;

import javax.servlet.ServletContext;

/**
 * Provides static access to a {@link com.atlassian.plugin.hostcontainer.HostContainer} instance.  Requires initialisation before first use.
 *
 * @since 2.2.0
 */
public class ServletContextHostContainerAccessor
{
    private static final String HOST_CONTAINER_KEY = ServletContextHostContainerAccessor.class.getPackage()+".hostcontainer";

    /**
     * Gets the host container for instance or thread
     *
     * @param servletContext the servlet context to look up the host container in
     * @return The host container instance
     * @throws IllegalStateException If it hasn't been initialised yet
     */
    public static HostContainer getHostContainer(ServletContext servletContext) throws IllegalStateException
    {
        return (HostContainer) servletContext.getAttribute(HOST_CONTAINER_KEY);
    }

    /**
     * Sets the implementation of the host container accessor
     *
     * @param servletContext the servlet context to set the container in
     * @param hostContainer the implementation to set
     */
    public static void setHostContainer(ServletContext servletContext, HostContainer hostContainer)
    {
        servletContext.setAttribute(HOST_CONTAINER_KEY, hostContainer);
    }

}
