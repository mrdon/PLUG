package com.atlassian.plugin;

import junit.framework.TestCase;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;

public class TestDefaultModuleDescriptorFactory extends TestCase
{
    private DefaultModuleDescriptorFactory moduleDescriptorFactory;

    protected void setUp() throws Exception
    {
        super.setUp();

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
    }

    public void testModuleDescriptorFactory() throws PluginParseException, IllegalAccessException, ClassNotFoundException, InstantiationException
    {
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        assertTrue(moduleDescriptorFactory.getModuleDescriptor("animal") instanceof MockAnimalModuleDescriptor);
        assertTrue(moduleDescriptorFactory.getModuleDescriptor("mineral") instanceof MockMineralModuleDescriptor);

        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("animal"));
        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("mineral"));
        assertFalse(moduleDescriptorFactory.hasModuleDescriptor("something"));

        // Test removing a module descriptor
        moduleDescriptorFactory.removeModuleDescriptorForType("mineral");

        // Ensure the removed module descriptor is not there
        assertFalse(moduleDescriptorFactory.hasModuleDescriptor("mineral"));

        // Ensure the other one is still there
        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("animal"));
        assertTrue(moduleDescriptorFactory.getModuleDescriptor("animal") instanceof MockAnimalModuleDescriptor);
    }
}
