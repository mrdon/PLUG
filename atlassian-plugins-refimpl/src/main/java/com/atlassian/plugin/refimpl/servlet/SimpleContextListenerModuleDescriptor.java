package com.atlassian.plugin.refimpl.servlet;

import com.atlassian.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;

public class SimpleContextListenerModuleDescriptor extends ServletContextListenerModuleDescriptor
{
    @Override
    protected void autowireObject(Object obj)
    {
        throw new UnsupportedOperationException("Only 2.0 plugins are supported");
    }
}
