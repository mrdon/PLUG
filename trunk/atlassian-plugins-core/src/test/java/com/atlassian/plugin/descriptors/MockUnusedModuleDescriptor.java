package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.mock.MockThing;

public class MockUnusedModuleDescriptor extends AbstractModuleDescriptor<MockThing>
{
    @Override
    public MockThing getModule()
    {
        throw new UnsupportedOperationException("You should never be getting a module from this descriptor " + this.getClass().getName());
    }
}
