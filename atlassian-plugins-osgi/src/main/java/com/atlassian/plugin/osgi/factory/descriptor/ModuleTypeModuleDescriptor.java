package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

import java.util.List;

/**
 * Module descriptor for dynamic module types.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
public class ModuleTypeModuleDescriptor extends AbstractModuleDescriptor<ModuleDescriptor<?>>
{
    public ModuleTypeModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public ModuleDescriptor<?> getModule()
    {
        throw new UnsupportedOperationException();
    }

    public void disableDependants(PluginAccessor pluginAccessor, PluginController pluginController) {
        loadClass(plugin, moduleClassName);
        final List<ModuleDescriptor<?>> dependants = pluginAccessor.getEnabledModuleDescriptorsByClass(moduleClass);
        for (final ModuleDescriptor<?> dependant :
                dependants) {
            pluginController.disablePluginModule(dependant.getCompleteKey());
        }
    }

}
