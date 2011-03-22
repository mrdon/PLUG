package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 *
 */
public class EventTrackingModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private volatile int enabledCount = 0;
    private volatile int disabledCount = 0;
    @Override
    public Void getModule()
    {
        return null;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        enabledCount++;
    }

    @Override
    public void disabled()
    {
        super.disabled();
        disabledCount++;
    }

    public int getEnabledCount()
    {
        return enabledCount;
    }

    public int getDisabledCount()
    {
        return disabledCount;
    }
}
