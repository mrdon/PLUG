package com.atlassian.plugin.descriptors;

public class MockUnusedModuleDescriptor extends AbstractModuleDescriptor
{
    public Object getModule()
    {
        throw new UnsupportedOperationException("You should never be getting a module from this descriptor " + this.getClass().getName());
    }
}
