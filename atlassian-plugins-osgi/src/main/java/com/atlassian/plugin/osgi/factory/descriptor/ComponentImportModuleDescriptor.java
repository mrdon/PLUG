package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.osgi.module.SpringModuleCreator;

/**
 * Module descriptor for OSGi service imports.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
public class ComponentImportModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    /**
     * @since 2.5.0
     */
    public ComponentImportModuleDescriptor()
    {
        super(ModuleClassFactory.NOOP_MODULE_CREATOR);
    }

    public Object getModule()
    {
        return new SpringModuleCreator().createBean(getKey(), this);
    }

}