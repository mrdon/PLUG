package com.atlassian.plugin.loaders;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.ResourcedModuleDescriptor;
import com.atlassian.plugin.mock.*;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSinglePluginLoader extends TestCase
{

    public void testAtlassianPlugin() throws Exception
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-atlassian-plugin.xml");
        Map moduleDescriptors = new HashMap();
        moduleDescriptors.put("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptors.put("mineral", MockMineralModuleDescriptor.class);
        Collection plugins = loader.getPlugins(moduleDescriptors);

        assertEquals(1, plugins.size());

        Plugin plugin = (Plugin) plugins.iterator().next();
        assertEquals("Test Plugin", plugin.getName());
        assertEquals("test.atlassian.plugin", plugin.getKey());
        assertEquals("This plugin descriptor is just used for test purposes!", plugin.getDescription());
        assertEquals(2, plugin.getModules().size());

        ResourcedModuleDescriptor bearDescriptor = (ResourcedModuleDescriptor) plugin.getModule("bear");
        assertEquals("bear", bearDescriptor.getKey());
        assertEquals("Bear Animal", bearDescriptor.getName());
        assertEquals(MockBear.class, bearDescriptor.getModuleClass());
        assertEquals("A plugin module to describe a bear", bearDescriptor.getDescription());

        List resources = bearDescriptor.getResourceDescriptors();
        assertEquals(3, resources.size());

        assertEquals("20", bearDescriptor.getParams().get("height"));
        assertEquals("brown", bearDescriptor.getParams().get("colour"));

        List goldDescriptors = plugin.getModulesByClass(MockGold.class);
        assertEquals(1, goldDescriptors.size());
        ModuleDescriptor goldDescriptor = (ModuleDescriptor) goldDescriptors.get(0);
        assertEquals(new MockGold(20), goldDescriptor.getModule());
        assertEquals(goldDescriptors, plugin.getModulesByClass(MockMineral.class));
    }

    public void testDisabledPlugin() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-disabled-plugin.xml");
        Collection plugins = loader.getPlugins(new HashMap());
        assertEquals(1, plugins.size());
        assertFalse(((Plugin) plugins.iterator().next()).isEnabledByDefault());
    }

    public void testPluginByUrl() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResource("test-disabled-plugin.xml", SinglePluginLoader.class));
        Collection plugins = loader.getPlugins(new HashMap());
        assertEquals(1, plugins.size());
        assertFalse(((Plugin) plugins.iterator().next()).isEnabledByDefault());
    }

    public void testUnfoundPlugin()
    {
        try
        {
            SinglePluginLoader loader = new SinglePluginLoader("bullshit.xml");
            fail("should have thrown exception.");
        }
        catch (PluginParseException e)
        {
            return;
        }
        fail("Didn't throw exception correctly");
    }

    public void testUnknownPluginModule() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-bad-plugin.xml");
        assertEquals(0, loader.getPlugins(new HashMap()).size());
    }

}
