package com.atlassian.plugin.osgi;

import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

public class StubServletModuleDescriptor extends ServletModuleDescriptor
{
    public StubServletModuleDescriptor()
    {
        this(ModuleFactory.LEGACY_MODULE_FACTORY, new DefaultServletModuleManager(new DefaultPluginEventManager()));
    }

    public StubServletModuleDescriptor(final ModuleFactory moduleCreator, final ServletModuleManager mgr)
    {
        super(moduleCreator, mgr);
    }
}
