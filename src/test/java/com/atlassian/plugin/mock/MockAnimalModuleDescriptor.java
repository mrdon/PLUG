package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.ResourcedModuleDescriptor;

public class MockAnimalModuleDescriptor extends ResourcedModuleDescriptor
{
    public Object getModule()
    {
        return new MockBear();
    }
}
