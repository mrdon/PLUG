package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.CannotDisable;
import com.atlassian.plugin.module.ModuleFactory;

/**
 * Module descriptor for dynamic module types.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
@CannotDisable
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

}
