package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;

public class DummyModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private boolean enabled = false;

    /**
     *
     * @param moduleCreator
     *
     * @since 2.5.0
     */
    public DummyModuleDescriptor(ModuleClassFactory moduleClassFactory)
    {
        super(moduleClassFactory);
    }

    @Override
    public Void getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        enabled = true;

    }

    public boolean isEnabled()
    {
        return enabled;
    }

}
