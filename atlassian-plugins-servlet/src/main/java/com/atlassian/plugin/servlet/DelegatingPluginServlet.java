package com.atlassian.plugin.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.plugin.servlet.descriptors.BaseServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.util.ClassLoaderStack;

/**
 * We are wrapping the plugins servlet in another servlet so that we can set some things up before
 * the plugins servlet is called. Currently we do the following:
 *      <ul>
 *        <li>the Threads classloader to the plugins classloader)</li>
 *        <li>wrap the request so that path info is right for the servlets</li>
 *      </ul>
 */
public class DelegatingPluginServlet extends HttpServlet
{
    private final ServletModuleDescriptor descriptor;

    private final HttpServlet servlet;

    public DelegatingPluginServlet(ServletModuleDescriptor descriptor)
    {
        this.descriptor = descriptor;
        this.servlet = descriptor.getModule();
    }

    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try
        {
            servlet.service(new PluginHttpRequestWrapper(req, descriptor), res);
        }
        finally
        {
            ClassLoaderStack.pop();
        }
    }

    public void init(ServletConfig config) throws ServletException
    {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try
        {
            servlet.init(config);
        }
        finally
        {
            ClassLoaderStack.pop();
        }
    }

    public void destroy()
    {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try
        {
            servlet.destroy();
        }
        finally
        {
            ClassLoaderStack.pop();
        }
    }

    public boolean equals(Object obj)
    {
        return servlet.equals(obj);
    }

    public String getInitParameter(String name)
    {
        return servlet.getInitParameter(name);
    }

    public Enumeration getInitParameterNames()
    {
        return servlet.getInitParameterNames();
    }

    public ServletConfig getServletConfig()
    {
        return servlet.getServletConfig();
    }

    public ServletContext getServletContext()
    {
        return servlet.getServletContext();
    }

    public String getServletInfo()
    {
        return servlet.getServletInfo();
    }

    public String getServletName()
    {
        return servlet.getServletName();
    }

    public int hashCode()
    {
        return servlet.hashCode();
    }

    public void init() throws ServletException
    {
        servlet.init();
    }

    public void log(String message, Throwable t)
    {
        servlet.log(message, t);
    }

    public void log(String msg)
    {
        servlet.log(msg);
    }

    public String toString()
    {
        return servlet.toString();
    }

    public BaseServletModuleDescriptor<?> getDescriptor()
    {
        return descriptor;
    }
}
