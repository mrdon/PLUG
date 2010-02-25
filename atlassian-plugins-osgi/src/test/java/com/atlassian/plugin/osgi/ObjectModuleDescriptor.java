package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;

/**
 * Module type for an object
 */
public class ObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    public ObjectModuleDescriptor()
    {
        super(ModuleClassFactory.NOOP_MODULE_CREATOR);
    }

    public ObjectModuleDescriptor(ModuleClassFactory moduleCreator)
    {
        super(moduleCreator);
    }

    public Object getModule()
    {
        return moduleClassFactory.createModuleClass(moduleClassName, this);
    }
}
