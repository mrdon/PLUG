/**
 * 
 */
package com.atlassian.plugin.descriptors.servlet;

import java.util.List;

import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.Plugin;

public class MockServletModuleDescriptor extends ServletModuleDescriptor
{
    final HttpServlet servlet;
    final Plugin plugin;
    final List paths;
    
    MockServletModuleDescriptor(Plugin plugin, HttpServlet servlet, List paths)
    {
        this.plugin = plugin;
        this.servlet = servlet;
        this.paths = paths;
    }
    
    public Plugin getPlugin()
    {
        return plugin;
    }

    public Object getModule()
    {
        return servlet;
    }
    
    public HttpServlet getServlet()
    {
        return servlet;
    }
    
    public List getPaths()
    {
        return paths;
    }
    
    public void enabled() {}
    public void disabled() {}
    protected void autowireObject(Object obj) {}
    protected ServletModuleManager getServletModuleManager() { return null; }
}