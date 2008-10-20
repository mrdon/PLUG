package com.atlassian.plugin.servlet;

import javax.servlet.ServletContext;

/**
 * A factory for providing access to a {@link ServletContext}.
 */
public interface ServletContextFactory
{
    public ServletContext getServletContext();
}