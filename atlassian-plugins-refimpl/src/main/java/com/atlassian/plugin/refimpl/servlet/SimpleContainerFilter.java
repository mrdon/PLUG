package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.ServletFilterModuleContainerFilter;

/**
 * This is a simple example of the {@link ServletFilterModuleContainerFilter}.  It uses the static 
 * {@link ContainerManager} to lookup the appropriate {@link ServletModuleManager}.  Other applications should use 
 * their specific method of getting a reference to the {@link ServletModuleManager} and returning it. 
 */
public class SimpleContainerFilter extends ServletFilterModuleContainerFilter
{
    @Override
    protected ServletModuleManager getServletModuleManager()
    {
        return ContainerManager.getInstance().getServletModuleManager();
    }
}
