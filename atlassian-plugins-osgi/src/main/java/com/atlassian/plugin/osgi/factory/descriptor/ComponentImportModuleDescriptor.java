package com.atlassian.plugin.osgi.factory.descriptor;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Module descriptor for OSGi service imports.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
public class ComponentImportModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    public Void getModule()
    {
        throw new UnsupportedOperationException();
    }

}