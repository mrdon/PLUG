package com.atlassian.plugin.osgi;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;

import javax.servlet.http.HttpServlet;

public class StubServletModuleDescriptor<T extends HttpServlet> extends ServletModuleDescriptor<T>
{
    public StubServletModuleDescriptor()
    {
        this(new DefaultServletModuleManager(new DefaultPluginEventManager()));
    }

    public StubServletModuleDescriptor(ServletModuleManager mgr)
    {
        super(new DefaultHostContainer(), mgr);
    }
}
