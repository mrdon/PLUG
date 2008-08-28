package com.atlassian.plugin.servlet;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import com.atlassian.plugin.servlet.descriptors.BaseServletModuleDescriptor;

public final class PluginServletConfig implements ServletConfig
{
    private final BaseServletModuleDescriptor<?> descriptor;
    private final ServletContext servletContext;

    public PluginServletConfig(BaseServletModuleDescriptor<?> descriptor, ServletContext servletContext)
    {
        this.descriptor = descriptor;
        this.servletContext = servletContext;
    }

    public String getServletName()
    {
        return descriptor.getName();
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }

    public String getInitParameter(String s)
    {
        return (String) descriptor.getInitParams().get(s);
    }

    public Enumeration getInitParameterNames()
    {
        return Collections.enumeration(descriptor.getInitParams().keySet());
    }
}