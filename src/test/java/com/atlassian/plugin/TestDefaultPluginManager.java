package com.atlassian.plugin;

import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockBear;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;
import com.atlassian.plugin.store.MemoryPluginStateStore;
import com.atlassian.plugin.util.FileUtils;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.ByteArrayInputStream;

public class TestDefaultPluginManager extends AbstractTestClassLoader
{
    private ClassLoadingPluginLoader classLoadingPluginLoader;
    private MemoryPluginStateStore stateStore;


    protected void tearDown() throws Exception
    {
        if (classLoadingPluginLoader != null)
        {
            classLoadingPluginLoader.shutDown();
        }

        super.tearDown();
    }

    public void testRetrievePlugins() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        assertEquals(2, manager.getPlugins().size());
        assertEquals(1, manager.getEnabledPlugins().size());
        manager.enablePlugin("test.disabled.plugin");
        assertEquals(2, manager.getEnabledPlugins().size());
    }

    public void testEnabledDisabledRetrieval() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("bullshit", MockUnusedModuleDescriptor.class);
        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        // check non existant plugins don't show
        assertNull(manager.getPlugin("bull:shit"));
        assertNull(manager.getEnabledPlugin("bull:shit"));
        assertNull(manager.getPluginModule("bull:shit"));
        assertNull(manager.getEnabledPluginModule("bull:shit"));
        assertTrue(manager.getEnabledModuleDescriptorsByClass(TestCase.class).isEmpty());
        assertTrue(manager.getEnabledModuleDescriptorsByType("bullshit").isEmpty());
        assertTrue(manager.getEnabledModulesByClass(TestCase.class).isEmpty());

        final String pluginKey = "test.atlassian.plugin";
        final String moduleKey = pluginKey + ":bear";

        // retrieve everything when enabled
        assertNotNull(manager.getPlugin(pluginKey));
        assertNotNull(manager.getEnabledPlugin(pluginKey));
        assertNotNull(manager.getPluginModule(moduleKey));
        assertNotNull(manager.getEnabledPluginModule(moduleKey));
        assertNull(manager.getEnabledPluginModule(pluginKey + ":shit"));
        assertFalse(manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(manager.getEnabledModuleDescriptorsByType("animal").isEmpty());
        assertFalse(manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertEquals(new MockBear(), manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).get(0));

        // now only retrieve via always retrieve methods
        manager.disablePlugin(pluginKey);
        assertNotNull(manager.getPlugin(pluginKey));
        assertNull(manager.getEnabledPlugin(pluginKey));
        assertNotNull(manager.getPluginModule(moduleKey));
        assertNull(manager.getEnabledPluginModule(moduleKey));
        assertTrue(manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertTrue(manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertTrue(manager.getEnabledModuleDescriptorsByType("animal").isEmpty());

        // now enable again and check back to start
        manager.enablePlugin(pluginKey);
        assertNotNull(manager.getPlugin(pluginKey));
        assertNotNull(manager.getEnabledPlugin(pluginKey));
        assertNotNull(manager.getPluginModule(moduleKey));
        assertNotNull(manager.getEnabledPluginModule(moduleKey));
        assertFalse(manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertFalse(manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(manager.getEnabledModuleDescriptorsByType("animal").isEmpty());

        // now let's disable the module, but not the plugin
        manager.disablePluginModule(moduleKey);
        assertNotNull(manager.getPlugin(pluginKey));
        assertNotNull(manager.getEnabledPlugin(pluginKey));
        assertNotNull(manager.getPluginModule(moduleKey));
        assertNull(manager.getEnabledPluginModule(moduleKey));
        assertTrue(manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertTrue(manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertTrue(manager.getEnabledModuleDescriptorsByType("animal").isEmpty());

        // now enable the module again
        manager.enablePluginModule(moduleKey);
        assertNotNull(manager.getPlugin(pluginKey));
        assertNotNull(manager.getEnabledPlugin(pluginKey));
        assertNotNull(manager.getPluginModule(moduleKey));
        assertNotNull(manager.getEnabledPluginModule(moduleKey));
        assertFalse(manager.getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertFalse(manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(manager.getEnabledModuleDescriptorsByType("animal").isEmpty());

    }

    public void testDuplicatePluginKeysAreBad() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();
        System.out.println(manager.getPlugins().size());
        assertTrue(manager.getPlugins().size() == 1);
    }

    public void testLoadOlderDuplicatePlugin() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin-newer.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();
        assertTrue(manager.getEnabledPlugins().size() == 1);
    }

    public void testLoadNewerDuplicatePlugin() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin-newer.xml"));
        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        try
        {
            manager.init();
            fail("Should have died with duplicate key exception.");
        }
        catch (PluginParseException e)
        {
            assertEquals("Duplicate plugin found (installed version is older) and could not be removed: 'test.atlassian.plugin'", e.getMessage());
        }
    }

    public void testGetPluginAndModules() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        Plugin plugin = manager.getPlugin("test.atlassian.plugin");
        assertNotNull(plugin);
        assertEquals("Test Plugin", plugin.getName());

        ModuleDescriptor bear = plugin.getModuleDescriptor("bear");
        assertEquals(bear, manager.getPluginModule("test.atlassian.plugin:bear"));
    }

    public void testGetModuleByModuleClassOneFound() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        Collection descriptors = manager.getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ModuleDescriptor moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bear Animal", moduleDescriptor.getName());

        descriptors = manager.getEnabledModuleDescriptorsByClass(MockMineralModuleDescriptor.class);
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bar", moduleDescriptor.getName());
    }

    public void testGetModuleByModuleClassNoneFound() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        final Collection descriptors = manager.getEnabledModulesByClass(java.lang.String.class);
        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());
    }

    public void testGetModuleDescriptorsByType() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        Collection descriptors = manager.getEnabledModuleDescriptorsByType("animal");
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ModuleDescriptor moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bear Animal", moduleDescriptor.getName());

        descriptors = manager.getEnabledModuleDescriptorsByType("mineral");
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        moduleDescriptor = (ModuleDescriptor) descriptors.iterator().next();
        assertEquals("Bar", moduleDescriptor.getName());

        try
        {
            manager.getEnabledModuleDescriptorsByType("foobar");
            fail("Should have thrown exception.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    public void testRetrievingDynamicResources() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        DefaultPluginManager manager = makeClassLoadingPluginManager();

        InputStream is = manager.getPluginResourceAsStream("test.atlassian.plugin.classloaded", "atlassian-plugin.xml");
        assertNotNull(is);
    }

    public void testFindingNewPlugins() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        //delete paddington for the timebeing
        File paddington = new File(pluginsTestDir, PADDINGTON_JAR);
        paddington.delete();

        DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(1, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));

        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);

        manager.scanForNewPlugins();
        assertEquals(2, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded"));

        manager.scanForNewPlugins();
        assertEquals(2, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded"));
    }

    private DefaultPluginManager makeClassLoadingPluginManager() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        classLoadingPluginLoader = new ClassLoadingPluginLoader(pluginsTestDir);
        pluginLoaders.add(classLoadingPluginLoader);
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        stateStore = new MemoryPluginStateStore();
        DefaultPluginManager manager = new DefaultPluginManager(stateStore, pluginLoaders, moduleDescriptorFactory);
        manager.init();
        return manager;
    }

    public void testRemovingPlugins() throws PluginException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        DefaultPluginManager manager = makeClassLoadingPluginManager();
        assertEquals(2, manager.getPlugins().size());
        MockAnimalModuleDescriptor moduleDescriptor = (MockAnimalModuleDescriptor) manager.getPluginModule("test.atlassian.plugin.classloaded:paddington");
        assertFalse(moduleDescriptor.disabled);
        manager.uninstall(manager.getPlugin("test.atlassian.plugin.classloaded"));
        assertTrue("Module must have had disable() called before being removed", moduleDescriptor.disabled);

        // uninstalling a plugin should remove it's state completely from the state store - PLUG-13
        assertNull(stateStore.loadPluginState().getState("test.atlassian.plugin.classloaded"));

        assertEquals(1, manager.getPlugins().size());
        assertNull(manager.getPlugin("test.atlassian.plugin.classloaded"));
        assertEquals(1, pluginsTestDir.listFiles().length);
    }

    public void testNonRemovablePlugins() throws PluginParseException
    {
        List pluginLoaders = new ArrayList();
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginStateStore(), pluginLoaders, moduleDescriptorFactory);
        manager.init();

        Plugin plugin = manager.getPlugin("test.atlassian.plugin");
        assertFalse(plugin.isUninstallable());
        assertNotNull(plugin.getResourceAsStream("test-atlassian-plugin.xml"));

        try
        {
            manager.uninstall(plugin);
            fail("Where was the exception?");
        }
        catch (PluginException p)
        {
        }
    }

    public void testNonDeletablePlugins() throws PluginException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        DefaultPluginManager manager = makeClassLoadingPluginManager();
        assertEquals(2, manager.getPlugins().size());

        // Set plugin file can't be deleted.
        DynamicPlugin pluginToRemove = (DynamicPlugin) manager.getPlugin("test.atlassian.plugin.classloaded");
        pluginToRemove.setDeletable(false);

        // Disable plugin module before uninstall
        MockAnimalModuleDescriptor moduleDescriptor = (MockAnimalModuleDescriptor) manager.getPluginModule("test.atlassian.plugin.classloaded:paddington");
        assertFalse(moduleDescriptor.disabled);

        manager.uninstall(pluginToRemove);

        assertTrue("Module must have had disable() called before being removed", moduleDescriptor.disabled);
        assertEquals(1, manager.getPlugins().size());
        assertNull(manager.getPlugin("test.atlassian.plugin.classloaded"));
        assertEquals(2, pluginsTestDir.listFiles().length);
    }



    // These methods test the plugin compareTo() function, which compares plugins based on their version numbers.
    public void testComparePluginNewer(){

        Plugin p1 = createPluginWithVersion("1.1");
        Plugin p2 = createPluginWithVersion("1.0");
        assertTrue(p1.compareTo(p2) == 1);

        p1.getPluginInformation().setVersion("1.10");
        p2.getPluginInformation().setVersion("1.2");
        assertTrue(p1.compareTo(p2) == 1);

        p1.getPluginInformation().setVersion("1.2");
        p2.getPluginInformation().setVersion("1.01");
        assertTrue(p1.compareTo(p2) == 1);

        p1.getPluginInformation().setVersion("1.0.1");
        p2.getPluginInformation().setVersion("1.0");
        assertTrue(p1.compareTo(p2) == 1);

        p1.getPluginInformation().setVersion("1.2");
        p2.getPluginInformation().setVersion("1.1.1");
        assertTrue(p1.compareTo(p2) == 1);
    }

    public void testComparePluginOlder(){

        Plugin p1 = createPluginWithVersion("1.0");
        Plugin p2 = createPluginWithVersion("1.1");
        assertTrue(p1.compareTo(p2) == -1);

        p1.getPluginInformation().setVersion("1.2");
        p2.getPluginInformation().setVersion("1.10");
        assertTrue(p1.compareTo(p2) == -1);

        p1.getPluginInformation().setVersion("1.01");
        p2.getPluginInformation().setVersion("1.2");
        assertTrue(p1.compareTo(p2) == -1);

        p1.getPluginInformation().setVersion("1.0");
        p2.getPluginInformation().setVersion("1.0.1");
        assertTrue(p1.compareTo(p2) == -1);

        p1.getPluginInformation().setVersion("1.1.1");
        p2.getPluginInformation().setVersion("1.2");
        assertTrue(p1.compareTo(p2) == -1);
    }

    public void testComparePluginEqual(){

        Plugin p1 = createPluginWithVersion("1.0");
        Plugin p2 = createPluginWithVersion("1.0");
        assertTrue(p1.compareTo(p2) == 0);

        p1.getPluginInformation().setVersion("1.1.0.0");
        p2.getPluginInformation().setVersion("1.1");
        assertTrue(p1.compareTo(p2) == 0);

        p1.getPluginInformation().setVersion(" 1 . 1 ");
        p2.getPluginInformation().setVersion("1.1");
        assertTrue(p1.compareTo(p2) == 0);
    }

    // If we can't understand the version of a plugin, then take the new one.
    public void testComparePluginNoVersion(){

        Plugin p1 = createPluginWithVersion("1.0");
        Plugin p2 = createPluginWithVersion("#$%");
        assertEquals(-1, p1.compareTo(p2));

        p1.getPluginInformation().setVersion("#$%");
        p2.getPluginInformation().setVersion("1.0");
        assertEquals(-1, p1.compareTo(p2));

    }

    public void testComparePluginBadPlugin(){

        Plugin p1 = createPluginWithVersion("1.0");
        Plugin p2 = createPluginWithVersion("1.0");

        // Compare against something with a different key
        p2.setKey("bad.key");
        assertTrue(p1.compareTo(p2) == 1);

        // Compare against something that isn't a plugin
        assertTrue(p1.compareTo("not a plugin") == 1);
    }

    public void testInvalidationOfDynamicResourceCache() throws IOException, PluginException
    {
        createFillAndCleanTempPluginDirectory();

        DefaultPluginManager manager = makeClassLoadingPluginManager();

        checkResources(manager, true, true);
        manager.disablePlugin("test.atlassian.plugin.classloaded");
        checkResources(manager, false, false);
        manager.enablePlugin("test.atlassian.plugin.classloaded");
        checkResources(manager, true, true);
        manager.uninstall(manager.getPlugin("test.atlassian.plugin.classloaded"));
        checkResources(manager, false, false);
        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);
        manager.scanForNewPlugins();
        checkResources(manager, true, true);
        // Resources from disabled modules are still available
        //manager.disablePluginModule("test.atlassian.plugin.classloaded:paddington");
        //checkResources(manager, true, false);
    }

    public void testInstallPlugin() throws Exception
    {
        Mock mockPluginStateStore = new Mock(PluginStateStore.class);
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        Mock mockPluginLoader = new Mock(PluginLoader.class);
        Mock mockDescriptorParserFactory = new Mock(DescriptorParserFactory.class);
        Mock mockDescriptorParser = new Mock(DescriptorParser.class);
        Mock mockPluginJar = new Mock(PluginJar.class);
        Mock mockRepository = new Mock(PluginInstaller.class);
        Mock mockPlugin = new Mock(Plugin.class);

        ModuleDescriptorFactory moduleDescriptorFactory = (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy();

        DefaultPluginManager pluginManager = new DefaultPluginManager(
                (PluginStateStore) mockPluginStateStore.proxy(),
                Collections.singletonList(mockPluginLoader.proxy()),
                moduleDescriptorFactory
        );

        Plugin plugin = (Plugin) mockPlugin.proxy();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        PluginJar pluginJar = (PluginJar) mockPluginJar.proxy();
        Object descriptorParser = mockDescriptorParser.proxy();

        mockPluginJar.expectAndReturn("getFile", "atlassian-plugin.xml", inputStream);
        mockPluginJar.expectAndReturn("getFile", "atlassian-plugin.xml", inputStream);
        mockPluginStateStore.expectAndReturn("loadPluginState", new PluginManagerState());
        mockDescriptorParserFactory.expectAndReturn("getInstance", inputStream, descriptorParser);
        mockDescriptorParserFactory.expectAndReturn("getInstance", inputStream, descriptorParser);
        mockDescriptorParser.matchAndReturn("getKey", "test");
        mockRepository.expect("installPlugin", C.args(C.eq("test"), C.eq(pluginJar)));
        mockPluginLoader.expectAndReturn("loadAllPlugins", C.eq(moduleDescriptorFactory), Collections.EMPTY_LIST);
        mockPluginLoader.expectAndReturn("supportsAddition", true);
        mockPluginLoader.expectAndReturn("addFoundPlugins", moduleDescriptorFactory, Collections.singletonList(plugin));
        mockPlugin.matchAndReturn("getKey", "test");
        mockPlugin.matchAndReturn("hashCode", mockPlugin.hashCode());
        mockPlugin.expectAndReturn("getModuleDescriptors", new ArrayList());
        mockPlugin.expectAndReturn("isEnabledByDefault", true);

        pluginManager.setDescriptorParserFactory((DescriptorParserFactory) mockDescriptorParserFactory.proxy());
        pluginManager.setPluginInstaller((PluginInstaller) mockRepository.proxy());
        pluginManager.init();
        pluginManager.installPlugin(pluginJar);

        assertEquals(plugin, pluginManager.getPlugin("test"));
        assertTrue(pluginManager.isPluginEnabled("test"));

        mockPlugin.verify();
        mockRepository.verify();
        mockPluginJar.verify();
        mockDescriptorParser.verify();
        mockDescriptorParserFactory.verify();
        mockPluginLoader.verify();
        mockPluginStateStore.verify();
    }

    private void checkResources(PluginAccessor manager, boolean canGetGlobal, boolean canGetModule)
    {
        InputStream is = manager.getDynamicResourceAsStream("icon.gif");
        assertEquals(canGetGlobal, is != null);
        is = manager.getDynamicResourceAsStream("bear/paddington.vm");
        assertEquals(canGetModule, is != null);
    }


    public Plugin createPluginWithVersion(String version){
        Plugin p = new StaticPlugin();
        p.setKey("test.default.plugin");
        p.setPluginInformation(new PluginInformation());
        PluginInformation pInfo = p.getPluginInformation();
        pInfo.setVersion(version);
        return p;
    }



}
