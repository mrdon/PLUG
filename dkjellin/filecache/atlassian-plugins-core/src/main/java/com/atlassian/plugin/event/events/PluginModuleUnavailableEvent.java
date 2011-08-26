package com.atlassian.plugin.event.events;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Signifies a plugin module is now unavailable outside the usual installation process.
 *
 * @since 2.5.0
 */
public class PluginModuleUnavailableEvent
{
    private final ModuleDescriptor module;

    public PluginModuleUnavailableEvent(ModuleDescriptor module)
    {
        this.module = module;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }
}