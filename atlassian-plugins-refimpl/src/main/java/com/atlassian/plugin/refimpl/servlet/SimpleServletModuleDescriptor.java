package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

/**
 * This is a simple example of the {@link ServletModuleDescriptor}.  The {@link #autowireObject} method
 * throws an {@link UnsupportedOperationException} meaning that it does not support servlet context listener modules
 * in plugins that are not marked version 2 plugins. 
 * <p/>
 * It uses the static {@link ContainerManager} to lookup the appropriate {@link ServletModuleManager}.  Other
 * applications should use their specific method of getting a reference to the {@link ServletModuleManager} and
 * returning it. 
 */
public class SimpleServletModuleDescriptor extends ServletModuleDescriptor
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
