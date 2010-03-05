package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

/**
 * Module type for an object
 */
public class ObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    public ObjectModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public ObjectModuleDescriptor(ModuleFactory moduleCreator)
    {
        super(moduleCreator);
    }

    public Object getModule()
    {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
