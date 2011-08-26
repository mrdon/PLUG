package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.mock.MockThing;
import com.atlassian.plugin.module.ModuleFactory;

public class MockUnusedModuleDescriptor extends AbstractModuleDescriptor<MockThing>
{
    public MockUnusedModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    @Override
    public MockThing getModule()
    {
        throw new UnsupportedOperationException("You should never be getting a module from this descriptor " + this.getClass().getName());
    }
}
