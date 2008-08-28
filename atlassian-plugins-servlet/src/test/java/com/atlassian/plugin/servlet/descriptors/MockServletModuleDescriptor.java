package com.atlassian.plugin.servlet.descriptors;

import java.util.List;

import javax.servlet.http.HttpServlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

public class MockServletModuleDescriptor extends ServletModuleDescriptor
{
    final HttpServlet servlet;
    final Plugin plugin;
    final List<String> paths;
    
    public MockServletModuleDescriptor(Plugin plugin, HttpServlet servlet, List<String> paths)
    {
        this.plugin = plugin;
        this.servlet = servlet;
        this.paths = paths;
    }
    
    public Plugin getPlugin()
    {
        return plugin;
    }

    public HttpServlet getModule()
    {
        return servlet;
    }
    
    public HttpServlet getServlet()
    {
        return servlet;
    }
    
    public List<String> getPaths()
    {
        return paths;
    }
    
    public void enabled() {}
    public void disabled() {}
    protected void autowireObject(Object obj) {}
    protected ServletModuleManager getServletModuleManager() { return null; }
}