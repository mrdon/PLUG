package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;

public class SimpleFilterModuleDescriptor extends ServletFilterModuleDescriptor
{
    protected void autowireObject(Object o) 
    {
        throw new UnsupportedOperationException("Only 2.0 plugins are supported");
    }

    protected ServletModuleManager getServletModuleManager()
    {
        return ContainerManager.getInstance().getServletModuleManager();
    }
}
