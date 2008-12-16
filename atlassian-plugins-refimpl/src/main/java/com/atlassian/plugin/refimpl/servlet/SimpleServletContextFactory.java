package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.servlet.ServletContextFactory;

import javax.servlet.ServletContext;

public class SimpleServletContextFactory implements ServletContextFactory
{
    private final ServletContext servletContext;

    public SimpleServletContextFactory(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }
}
