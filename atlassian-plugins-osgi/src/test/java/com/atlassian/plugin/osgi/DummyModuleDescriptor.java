package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

public class DummyModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private boolean enabled = false;

    /**
     *
     * @param moduleFactory
     *
     * @since 2.5.0
     */
    public DummyModuleDescriptor(ModuleFactory moduleFactory)
    {
        super(moduleFactory);
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
