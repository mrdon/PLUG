package com.atlassian.plugin.osgi;

import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

public class StubServletModuleDescriptor extends ServletModuleDescriptor
{
    public StubServletModuleDescriptor()
    {
        this(new DefaultServletModuleManager(new DefaultPluginEventManager()));
    }

    public StubServletModuleDescriptor(final ServletModuleManager mgr)
    {
        super(new DefaultHostContainer(), mgr);
    }
}
