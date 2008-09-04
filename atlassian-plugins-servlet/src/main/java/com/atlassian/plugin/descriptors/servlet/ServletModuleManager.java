package com.atlassian.plugin.descriptors.servlet;

import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;

@Deprecated
public class ServletModuleManager extends DefaultServletModuleManager
{
    public ServletModuleManager(PluginEventManager pluginEventManager)
    {
        super(pluginEventManager);
    }
}
