package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.module.ModuleClassFactory;

/**
 * @deprecated All module descriptors now have resources. Use AbstractModuleDescriptor instead.
 */
@Deprecated
public abstract class ResourcedModuleDescriptor<T> extends AbstractModuleDescriptor<T>
{
    public ResourcedModuleDescriptor()
    {
        super(ModuleClassFactory.LEGACY_MODULE_CLASS_FACTORY);
    }
}
