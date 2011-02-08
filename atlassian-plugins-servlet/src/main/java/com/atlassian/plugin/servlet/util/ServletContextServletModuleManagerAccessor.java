package com.atlassian.plugin.servlet.util;

import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.servlet.ServletModuleManager;

import javax.servlet.ServletContext;

/**
 * Provides static access to a {@link com.atlassian.plugin.hostcontainer.HostContainer} instance.  Requires initialisation before first use.
 *
 * @since 2.2.0
 */
public class ServletContextServletModuleManagerAccessor
{
    private static final String SERVLET_MODULE_MANAGER_KEY = ServletContextServletModuleManagerAccessor.class.getPackage()+".servletModuleManager";

    /**
     * Gets the servlet module manager in the servlet context
     *
     * @param servletContext the servlet context to look up the servlet module manager in
     * @return The servlet module manager instance or null if it is not there
     */
    public static ServletModuleManager getServletModuleManager(ServletContext servletContext) throws IllegalStateException
    {
        return (ServletModuleManager) servletContext.getAttribute(SERVLET_MODULE_MANAGER_KEY);
    }

    /**
     * Sets the implementation of the servlet module manager
     *
     * @param servletContext the servlet context to set the manager in
     * @param servletModuleManager the implementation to set
     */
    public static void setServletModuleManager(ServletContext servletContext, ServletModuleManager servletModuleManager)
    {
        servletContext.setAttribute(SERVLET_MODULE_MANAGER_KEY, servletModuleManager);
    }

}
