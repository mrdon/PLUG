package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;

/**
 * This is a simple example of the {@link ServletContextListenerModuleDescriptor}.  The {@link #autowireObject} method
 * throws an {@link UnsupportedOperationException} meaning that it does not support servlet context listener modules
 * in plugins that are not marked version 2 plugins. 
 */
public class SimpleContextListenerModuleDescriptor extends ServletContextListenerModuleDescriptor
{
    @Override
    protected void autowireObject(Object obj)
    {
        throw new UnsupportedOperationException("Only 2.0 plugins are supported");
    }
}
