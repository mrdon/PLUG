package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.CannotDisable;

@CannotDisable
public class MockVegetableModuleDescriptor extends AbstractModuleDescriptor<MockThing>
{
    @Override
    public MockThing getModule()
    {
        return new MockVegetable();
    }
}
