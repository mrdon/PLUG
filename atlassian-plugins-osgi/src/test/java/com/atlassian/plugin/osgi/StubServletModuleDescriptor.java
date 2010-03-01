package com.atlassian.plugin.osgi;

import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;

public class StubServletModuleDescriptor extends ServletModuleDescriptor
{
    public StubServletModuleDescriptor()
    {
        this(ModuleClassFactory.LEGACY_MODULE_CLASS_FACTORY, new DefaultServletModuleManager(new DefaultPluginEventManager()));
    }

    public StubServletModuleDescriptor(final ModuleClassFactory moduleCreator, final ServletModuleManager mgr)
    {
        super(moduleCreator, mgr);
    }
}
