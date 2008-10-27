package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

public class DummyModuleDescriptor extends AbstractModuleDescriptor
{
    private boolean enabled = false;
    public Object getModule()
    {
        return null;
    }

    public void enabled()
    {
        super.enabled();
        this.enabled = true;

    }

    public boolean isEnabled()
    {
        return enabled;
    }

}

