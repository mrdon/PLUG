package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.CannotDisable;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.module.BeanPrefixModuleFactory;

/**
 * Module descriptor for OSGi service imports.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
@CannotDisable
public class ComponentImportModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    /**
     * @since 2.5.0
     */
    public ComponentImportModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public Object getModule()
    {
        return new BeanPrefixModuleFactory().createModule(getKey(), this);
    }

}
