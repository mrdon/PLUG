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

    private final boolean persistent;

    public PluginModuleDisabledEvent(ModuleDescriptor module, boolean persistent)
    {
        this.module = module;
        this.persistent = persistent;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }

    /**
     * @return <code>true</code> iff this disabling will be persistent, i.e. it is not a transient, such as for an
     *  upgrade.
     */
    public boolean isPersistent() {
        return persistent;
    }

}