/**
 * 
 */
package com.atlassian.plugin.descriptors.servlet;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

final class PluginServetConfig implements ServletConfig
{
    private final ServletModuleDescriptor descriptor;
    private final ServletContext context;

    PluginServetConfig(ServletModuleDescriptor descriptor, ServletConfig servletConfig)
    {
        this.descriptor = descriptor;
        context = new PluginServletContextWrapper(descriptor, servletConfig.getServletContext());
    }

    public String getServletName()
    {
        return descriptor.getName();
    }

    public ServletContext getServletContext()
    {
        return context;
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