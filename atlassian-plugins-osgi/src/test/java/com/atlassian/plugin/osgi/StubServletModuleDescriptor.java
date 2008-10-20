package com.atlassian.plugin.osgi;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;

public class StubServletModuleDescriptor extends ServletModuleDescriptor
{
    protected void autowireObject(Object obj)
    {
    }

    protected ServletModuleManager getServletModuleManager()
    {
        return new DefaultServletModuleManager(new DefaultPluginEventManager());
    }
}