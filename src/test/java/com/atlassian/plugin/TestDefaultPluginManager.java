package com.atlassian.plugin;

import junit.framework.TestCase;

import java.util.*;

import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;

public class TestDefaultPluginManager extends TestCase
{
    public void testDuplicatePluginKeysAreBad() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));

        PluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, new HashMap());
        try
        {
            manager.init();
            fail("Should have died with duplicate key exception.");
        }
        catch (PluginParseException e)
        {
            assertEquals("Duplicate plugin key found: 'test.disabled.plugin'", e.getMessage());
        }
    }

    public void testGetPluginAndModules() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        Map moduleDescriptors = new HashMap();
        moduleDescriptors.put("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptors.put("mineral", MockMineralModuleDescriptor.class);

        PluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptors);
        manager.init();

        Plugin plugin = manager.getPlugin("test.atlassian.plugin");
        assertNotNull(plugin);
        assertEquals("Test Plugin", plugin.getName());

        ModuleDescriptor bear = plugin.getModule("bear");
        assertEquals(bear, manager.getPluginModule("test.atlassian.plugin:bear"));
    }

    public void testGetModuleByModuleClassOneFound() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        Map moduleDescriptors = new HashMap();
        moduleDescriptors.put("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptors.put("mineral", MockMineralModuleDescriptor.class);

        PluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptors);
        manager.init();

        Collection descriptors = manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ModuleDescriptor moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bear Animal", moduleDescriptor.getName());

        descriptors = manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockGold.class);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bar", moduleDescriptor.getName());
    }
    
    public void testGetModuleByModuleClassNoneFound() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        Map moduleDescriptors = new HashMap();
        moduleDescriptors.put("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptors.put("mineral", MockMineralModuleDescriptor.class);

        PluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptors);
        manager.init();

        final Collection descriptors = manager.getEnabledModulesByClass(java.lang.String.class);
        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());
    }
}
