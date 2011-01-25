package com.atlassian.plugin.osgi.module;

import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.osgi.factory.descriptor.ModuleTypeModuleDescriptor;

import java.util.List;

public class ModuleTypeDependantsDisabler {

    private volatile PluginAccessor pluginAccessor;

    private volatile PluginController pluginController;

    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    public void setPluginController(PluginController pluginController) {
        this.pluginController = pluginController;
    }

    @EventListener
    public void onPluginModuleDisabled(PluginModuleDisabledEvent event) {
        if (event.isPersistent()) {
            final ModuleDescriptor moduleDescriptor = event.getModule();
            if (moduleDescriptor instanceof ModuleTypeModuleDescriptor) {
                ((ModuleTypeModuleDescriptor) moduleDescriptor).disableDependants(pluginAccessor, pluginController);
            }
        }
    }

}
