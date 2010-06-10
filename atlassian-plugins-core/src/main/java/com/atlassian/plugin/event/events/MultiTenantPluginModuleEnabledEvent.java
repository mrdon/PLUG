package com.atlassian.plugin.event.events;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Event fired when a plugin module is enabled for a particular tenant (it should already be enabled for the 
 * plugin system)
 */
public class MultiTenantPluginModuleEnabledEvent
{
    private final ModuleDescriptor module;

    public MultiTenantPluginModuleEnabledEvent(ModuleDescriptor module)
    {
        this.module = module;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }
}