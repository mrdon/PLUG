package com.atlassian.plugin.osgi;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;

import javax.servlet.http.HttpServlet;

public class StubServletModuleDescriptor<T extends HttpServlet> extends ServletModuleDescriptor<T>
{
    private final ServletModuleManager mgr;

    public StubServletModuleDescriptor()
    {
        this.mgr = new DefaultServletModuleManager(new DefaultPluginEventManager());
    }

    public StubServletModuleDescriptor(ServletModuleManager mgr)
    {
        this.mgr = mgr;
    }

    protected void autowireObject(Object obj)
    {
    }

    protected ServletModuleManager getServletModuleManager()
    {
        return mgr;
    }
}
