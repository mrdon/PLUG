package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.ServletFilterModuleContainerFilter;

public class SimpleContainerFilter extends ServletFilterModuleContainerFilter
{
    @Override
    protected ServletModuleManager getServletModuleManager()
    {
        return ContainerManager.getInstance().getServletModuleManager();
    }
}
