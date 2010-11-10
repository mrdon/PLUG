package com.atlassian.plugin.event.events;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Event fired when a plugin module is disabled, which can also happen when its
 * plugin is disabled or uninstalled.
 * 
 * @see com.atlassian.plugin.event.events
 */
public class PluginModuleDisabledEvent
{
    private final ModuleDescriptor module;

    public PluginModuleDisabledEvent(ModuleDescriptor module)
    {
        this.module = module;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }
}