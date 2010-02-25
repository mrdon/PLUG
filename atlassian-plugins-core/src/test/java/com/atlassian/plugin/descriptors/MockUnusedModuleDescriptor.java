package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.mock.MockThing;
import com.atlassian.plugin.module.ModuleClassFactory;

public class MockUnusedModuleDescriptor extends AbstractModuleDescriptor<MockThing>
{
    public MockUnusedModuleDescriptor()
    {
        super(ModuleClassFactory.NOOP_MODULE_CREATOR);
    }

    @Override
    public MockThing getModule()
    {
        throw new UnsupportedOperationException("You should never be getting a module from this descriptor " + this.getClass().getName());
    }
}
