package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.mock.*;
import com.atlassian.plugin.util.ClassLoaderUtils;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TestSinglePluginLoader extends TestCase
{
    public void testSinglePluginLoader() throws Exception
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-system-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        Plugin plugin = (Plugin) plugins.iterator().next();
        assertTrue(plugin.isSystemPlugin());
    }

    public void testRejectOsgiPlugin() throws Exception
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-osgi-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        Plugin plugin = plugins.iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        assertEquals("test.atlassian.plugin", plugin.getKey());
    }

    public void testAtlassianPlugin() throws Exception
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-atlassian-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        Plugin plugin = (Plugin) plugins.iterator().next();
        enableModules(plugin);
        assertEquals("Test Plugin", plugin.getName());
        assertEquals("test.atlassian.plugin", plugin.getKey());
        assertNotNull(plugin.getPluginInformation());
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertEquals("test.atlassian.plugin.i18n", plugin.getI18nNameKey());
        assertEquals("test.atlassian.plugin.desc.i18n", plugin.getPluginInformation().getDescriptionKey());
        assertEquals("This plugin descriptor is just used for test purposes!", plugin.getPluginInformation().getDescription());
        assertEquals("Atlassian Software Systems Pty Ltd", plugin.getPluginInformation().getVendorName());
        assertEquals("http://www.atlassian.com", plugin.getPluginInformation().getVendorUrl());
        assertEquals(3f, plugin.getPluginInformation().getMinVersion(), 0);
        assertEquals(3.1f, plugin.getPluginInformation().getMaxVersion(), 0);
        assertEquals(4, plugin.getModuleDescriptors().size());

        ModuleDescriptor bearDescriptor = plugin.getModuleDescriptor("bear");
        assertEquals("test.atlassian.plugin:bear", bearDescriptor.getCompleteKey());
        assertEquals("bear", bearDescriptor.getKey());
        assertEquals("Bear Animal", bearDescriptor.getName());
        assertEquals(MockBear.class, bearDescriptor.getModuleClass());
        assertEquals("A plugin module to describe a bear", bearDescriptor.getDescription());
        assertTrue(bearDescriptor.isEnabledByDefault());
        assertEquals("test.atlassian.module.bear.name", bearDescriptor.getI18nNameKey());
        assertEquals("test.atlassian.module.bear.description", bearDescriptor.getDescriptionKey());

        List resources = bearDescriptor.getResourceDescriptors();
        assertEquals(3, resources.size());

        assertEquals("20", bearDescriptor.getParams().get("height"));
        assertEquals("brown", bearDescriptor.getParams().get("colour"));

        List goldDescriptors = plugin.getModuleDescriptorsByModuleClass(MockGold.class);
        assertEquals(1, goldDescriptors.size());
        ModuleDescriptor goldDescriptor = (ModuleDescriptor) goldDescriptors.get(0);
        assertEquals("test.atlassian.plugin:gold", goldDescriptor.getCompleteKey());
        assertEquals(new MockGold(20), goldDescriptor.getModule());
        assertEquals(goldDescriptors, plugin.getModuleDescriptorsByModuleClass(MockMineral.class));

        assertEquals(1, plugin.getResourceDescriptors().size());
        ResourceLocation pluginResource = plugin.getResourceLocation("download", "icon.gif");
        assertEquals("/icon.gif", pluginResource.getLocation());
    }

    public void testDisabledPlugin() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-disabled-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        final Plugin plugin = (Plugin) plugins.iterator().next();
        assertFalse(plugin.isEnabledByDefault());

        assertEquals(1, plugin.getModuleDescriptors().size());
        final ModuleDescriptor module = plugin.getModuleDescriptor("gold");
        assertFalse(module.isEnabledByDefault());
    }

    public void testPluginByUrl() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResource("test-disabled-plugin.xml", SinglePluginLoader.class));
        // URL created should be reentrant and create a different stream each time
        assertNotSame(loader.getSource(), loader.getSource());
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertFalse(((Plugin) plugins.iterator().next()).isEnabledByDefault());
    }

    /**
     * @deprecated testing deprecated behaviour
     */
    public void testPluginByInputStream() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResourceAsStream("test-disabled-plugin.xml", SinglePluginLoader.class));
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertFalse(((Plugin) plugins.iterator().next()).isEnabledByDefault());
    }

    /**
     * @deprecated testing deprecated behaviour
     */
    public void testPluginByInputStreamNotReentrant() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResourceAsStream("test-disabled-plugin.xml", SinglePluginLoader.class));
        loader.getSource();
        try
        {
            loader.getSource();
            fail("IllegalStateException expected");
        }
        catch (IllegalStateException expected)
        {}
    }

    public void testPluginsInOrder() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResource("test-ordered-pluginmodules.xml", SinglePluginLoader.class));
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        final Plugin plugin = (Plugin) plugins.iterator().next();
        Collection modules = plugin.getModuleDescriptors();
        assertEquals(3, modules.size());
        Iterator iterator = modules.iterator();
        assertEquals("yogi1", ((MockAnimalModuleDescriptor)iterator.next()).getKey());
        assertEquals("yogi2", ((MockAnimalModuleDescriptor)iterator.next()).getKey());
        assertEquals("yogi3", ((MockAnimalModuleDescriptor)iterator.next()).getKey());
    }

    public void testUnknownPluginModule() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-bad-plugin.xml");
        Collection plugins = loader.loadAllPlugins(new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
        List pluginsList = new ArrayList(plugins);

        assertEquals(1, pluginsList.size());

        Plugin plugin = (Plugin) plugins.iterator().next();
        List moduleList = new ArrayList(plugin.getModuleDescriptors());

        // The module that had the problem should be an UnrecognisedModuleDescriptor
        assertEquals(UnrecognisedModuleDescriptor.class, moduleList.get(0).getClass());
    }

    // PLUG-5
    public void testPluginWithOnlyPermittedModules() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-atlassian-plugin.xml");

        // Define the module descriptor factory
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        // Exclude mineral
        List permittedList = new ArrayList();
        permittedList.add("animal");
        moduleDescriptorFactory.setPermittedModuleKeys(permittedList);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        // 1 plugin returned
        assertEquals(1, plugins.size());

        Plugin plugin = (Plugin) plugins.iterator().next();

        // Only one descriptor, animal
        assertEquals(1, plugin.getModuleDescriptors().size());
        assertNotNull(plugin.getModuleDescriptor("bear"));
        assertNull(plugin.getModuleDescriptor("gold"));
    }

    // PLUG-5
    public void testPluginWithOnlyPermittedModulesAndMissingModuleDescriptor() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-atlassian-plugin.xml");

        // Define the module descriptor factory
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);

        // Exclude mineral
        List permittedList = new ArrayList();
        permittedList.add("animal");
        moduleDescriptorFactory.setPermittedModuleKeys(permittedList);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        // 1 plugin returned
        assertEquals(1, plugins.size());

        Plugin plugin = (Plugin) plugins.iterator().next();

        // Only one descriptor, animal
        assertEquals(1, plugin.getModuleDescriptors().size());
        assertNotNull(plugin.getModuleDescriptor("bear"));
        assertNull(plugin.getModuleDescriptor("gold"));
    }

    public void testBadPluginKey() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-bad-plugin-key-plugin.xml");
        Collection<Plugin> plugins = loader.loadAllPlugins(null);
        assertEquals(1, plugins.size());
        Plugin plugin = plugins.iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        assertEquals("test-bad-plugin-key-plugin.xml", plugin.getKey());
        assertTrue(((UnloadablePlugin)plugin).getErrorText().endsWith("Plugin keys cannot contain ':'. Key is 'test:bad'"));
    }

    public void testNonUniqueKeysWithinAPlugin() throws PluginParseException
    {
        SinglePluginLoader loader = new SinglePluginLoader("test-bad-non-unique-keys-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        List<Plugin> plugins = new ArrayList<Plugin>(loader.loadAllPlugins(moduleDescriptorFactory));
        assertEquals(1, plugins.size());
        Plugin plugin = plugins.get(0);
        assertTrue(plugin instanceof UnloadablePlugin);
        assertTrue(((UnloadablePlugin)plugin).getErrorText().endsWith("Found duplicate key 'bear' within plugin 'test.bad.plugin'"));
    }

    public void testBadResource()
    {
        List<Plugin> plugins = new ArrayList<Plugin>(new SinglePluginLoader("foo").loadAllPlugins(null));
        assertEquals(1, plugins.size());
        assertTrue(plugins.get(0) instanceof UnloadablePlugin);
        assertEquals("foo", plugins.get(0).getKey());
    }

    public void enableModules(Plugin plugin)
    {
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (descriptor instanceof StateAware)
            {
                ((StateAware)descriptor).enabled();
            }
        }
    }
}

