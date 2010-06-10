package com.atlassian.plugin.event.events;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Event fired when a plugin module is disabled for a tenant, but not for the plugin system
 */
public class MultiTenantPluginModuleDisabledEvent
{
    private final ModuleDescriptor module;

    public MultiTenantPluginModuleDisabledEvent(ModuleDescriptor module)
    {
        this.module = module;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }
}