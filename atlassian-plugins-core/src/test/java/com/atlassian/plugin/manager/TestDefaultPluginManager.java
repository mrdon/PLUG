package com.atlassian.plugin.manager;

import com.atlassian.plugin.*;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.descriptors.MockUnusedModuleDescriptor;
import com.atlassian.plugin.descriptors.RequiresRestart;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginContainerUnavailableEvent;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.event.events.PluginModuleAvailableEvent;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.event.events.PluginModuleUnavailableEvent;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.event.listeners.FailListener;
import com.atlassian.plugin.event.listeners.PassListener;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.factories.XmlDynamicPluginFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.DynamicPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.metadata.ClasspathFilePluginMetadata;
import com.atlassian.plugin.mock.*;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;
import com.atlassian.plugin.predicate.PluginPredicate;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.*;

public class TestDefaultPluginManager extends AbstractTestClassLoader
{
    /**
     * the object being tested
     */
    protected DefaultPluginManager manager;

    private PluginPersistentStateStore pluginStateStore;
    private List<PluginLoader> pluginLoaders;
    private DefaultModuleDescriptorFactory moduleDescriptorFactory; // we should be able to use the interface here?

    private DirectoryPluginLoader directoryPluginLoader;
    protected PluginEventManager pluginEventManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        pluginEventManager = new DefaultPluginEventManager();

        pluginStateStore = new MemoryPluginPersistentStateStore();
        pluginLoaders = new ArrayList<PluginLoader>();
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());

        manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory, pluginEventManager);
    }

    @Override
    protected void tearDown() throws Exception
    {
        manager = null;
        moduleDescriptorFactory = null;
        pluginLoaders = null;
        pluginStateStore = null;

        if (directoryPluginLoader != null)
        {
            directoryPluginLoader = null;
        }

        super.tearDown();
    }

    protected PluginAccessor getPluginAccessor()
    {
        return manager;
    }

    public void testRetrievePlugins() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        manager.init();

        assertEquals(2, manager.getPlugins().size());
        assertEquals(1, manager.getEnabledPlugins().size());
        manager.enablePlugin("test.disabled.plugin");
        assertEquals(2, manager.getEnabledPlugins().size());
    }

    public void testEnableModuleFailed() throws PluginParseException
    {
        final Mock mockPluginLoader = new Mock(PluginLoader.class);
        final ModuleDescriptor<Object> badModuleDescriptor = new AbstractModuleDescriptor<Object>(ModuleFactory.LEGACY_MODULE_FACTORY)
        {
            @Override
            public String getKey()
            {
                return "bar";
            }

            @Override
            public String getCompleteKey()
            {
                return "foo:bar";
            }

            @Override
            public void enabled()
            {
                throw new IllegalArgumentException("Cannot enable");
            }

            @Override
            public Object getModule()
            {
                return null;
            }
        };

        final AbstractModuleDescriptor goodModuleDescriptor = mock(AbstractModuleDescriptor.class);
        when(goodModuleDescriptor.getKey()).thenReturn("baz");
        when(goodModuleDescriptor.getCompleteKey()).thenReturn("foo:baz");

        Plugin plugin = new StaticPlugin()
        {
            @Override
            public Collection<ModuleDescriptor<?>> getModuleDescriptors()
            {
                return Arrays.<ModuleDescriptor<?>>asList(goodModuleDescriptor, badModuleDescriptor);
            }

            @Override
            public ModuleDescriptor<Object> getModuleDescriptor(final String key)
            {
                return badModuleDescriptor;
            }
        };
        plugin.setKey("foo");
        plugin.setEnabledByDefault(true);
        plugin.setPluginInformation(new PluginInformation());

        mockPluginLoader.expectAndReturn("loadAllPlugins", C.ANY_ARGS, Collections.singletonList(plugin));

        @SuppressWarnings("unchecked")
        final PluginLoader loader = (PluginLoader) mockPluginLoader.proxy();
        pluginLoaders.add(loader);

        pluginEventManager.register(new FailListener(PluginEnabledEvent.class));
        //pluginEventManager.register(new FailListener(PluginModuleEnabledEvent.class));

        MyModuleDisabledListener listener = new MyModuleDisabledListener(goodModuleDescriptor);
        pluginEventManager.register(listener);
        manager.init();


        assertEquals(1, getPluginAccessor().getPlugins().size());
        assertEquals(0, getPluginAccessor().getEnabledPlugins().size());
        plugin = getPluginAccessor().getPlugin("foo");
        assertFalse(plugin.getPluginState() == PluginState.ENABLED);
        assertTrue(plugin instanceof UnloadablePlugin);
        assertTrue(listener.isCalled());
    }

    public static class MyModuleDisabledListener
    {
        private final ModuleDescriptor goodModuleDescriptor;
        private volatile boolean disableCalled = false;

        public MyModuleDisabledListener(ModuleDescriptor goodModuleDescriptor)
        {
            this.goodModuleDescriptor = goodModuleDescriptor;
        }

        @PluginEventListener
        public void onDisable(PluginModuleDisabledEvent evt)
        {
            if (evt.getModule().equals(goodModuleDescriptor))
            {
                disableCalled = true;
            }
        }

        public boolean isCalled()
        {
            return disableCalled;
        }
    }

    public void testEnabledModuleOutOfSyncWithPlugin() throws PluginParseException
    {
        final Mock mockPluginLoader = new Mock(PluginLoader.class);
        Plugin plugin = new StaticPlugin();
        plugin.setKey("foo");
        plugin.setEnabledByDefault(true);
        plugin.setPluginInformation(new PluginInformation());

        mockPluginLoader.expectAndReturn("loadAllPlugins", C.ANY_ARGS, Collections.singletonList(plugin));

        @SuppressWarnings("unchecked")
        final PluginLoader loader = (PluginLoader) mockPluginLoader.proxy();
        pluginLoaders.add(loader);
        manager.init();

        assertEquals(1, getPluginAccessor().getPlugins().size());
        assertEquals(1, getPluginAccessor().getEnabledPlugins().size());
        plugin = getPluginAccessor().getPlugin("foo");
        assertTrue(plugin.getPluginState() == PluginState.ENABLED);
        assertTrue(getPluginAccessor().isPluginEnabled("foo"));
        plugin.disable();
        assertFalse(plugin.getPluginState() == PluginState.ENABLED);
        assertFalse(getPluginAccessor().isPluginEnabled("foo"));
    }

    public void testDisablePluginModuleWithCannotDisableAnnotation()
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("bullshit", MockUnusedModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);

        manager.init();

        final String pluginKey = "test.atlassian.plugin";
        final String disablableModuleKey = pluginKey + ":bear";
        final String moduleKey = pluginKey + ":veg";

        // First, make sure we can disable the bear module
        manager.disablePluginModule(disablableModuleKey);
        assertNull(getPluginAccessor().getEnabledPluginModule(disablableModuleKey));

        // Now, make sure we can't disable the veg module
        manager.disablePluginModule(moduleKey);
        assertNotNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
    }

    public void testDisablePluginModuleWithCannotDisableAnnotationinSuperclass()
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("bullshit", MockUnusedModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetableSubclass", MockVegetableSubclassModuleDescriptor.class);

        manager.init();

        final String pluginKey = "test.atlassian.plugin";
        final String disablableModuleKey = pluginKey + ":bear";
        final String moduleKey = pluginKey + ":vegSubclass";

        // First, make sure we can disable the bear module
        manager.disablePluginModule(disablableModuleKey);
        assertNull(getPluginAccessor().getEnabledPluginModule(disablableModuleKey));

        // Now, make sure we can't disable the vegSubclass module
        manager.disablePluginModule(moduleKey);
        assertNotNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
    }

    public void testEnabledDisabledRetrieval() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("bullshit", MockUnusedModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);

        final PassListener enabledListener = new PassListener(PluginEnabledEvent.class);
        final PassListener disabledListener = new PassListener(PluginDisabledEvent.class);
        pluginEventManager.register(enabledListener);
        pluginEventManager.register(disabledListener);

        manager.init();

        // check non existent plugins don't show
        assertNull(getPluginAccessor().getPlugin("bull:shit"));
        assertNull(getPluginAccessor().getEnabledPlugin("bull:shit"));
        assertNull(getPluginAccessor().getPluginModule("bull:shit"));
        assertNull(getPluginAccessor().getEnabledPluginModule("bull:shit"));
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByClass(NothingModuleDescriptor.class).isEmpty());
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByType("bullshit").isEmpty());

        final String pluginKey = "test.atlassian.plugin";
        final String moduleKey = pluginKey + ":bear";

        // retrieve everything when enabled
        assertNotNull(getPluginAccessor().getPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getEnabledPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getPluginModule(moduleKey));
        assertNotNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
        assertNull(getPluginAccessor().getEnabledPluginModule(pluginKey + ":shit"));
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByType("animal").isEmpty());
        assertFalse(getPluginAccessor().getEnabledModulesByClass(MockBear.class).isEmpty());
        assertEquals(new MockBear(), getPluginAccessor().getEnabledModulesByClass(MockBear.class).get(0));
        enabledListener.assertCalled();

        // now only retrieve via always retrieve methods
        manager.disablePlugin(pluginKey);
        assertNotNull(getPluginAccessor().getPlugin(pluginKey));
        assertNull(getPluginAccessor().getEnabledPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getPluginModule(moduleKey));
        assertNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
        assertTrue(getPluginAccessor().getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByType("animal").isEmpty());
        disabledListener.assertCalled();

        // now enable again and check back to start
        manager.enablePlugin(pluginKey);
        assertNotNull(getPluginAccessor().getPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getEnabledPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getPluginModule(moduleKey));
        assertNotNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
        assertFalse(getPluginAccessor().getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByType("animal").isEmpty());
        enabledListener.assertCalled();

        // now let's disable the module, but not the plugin
        pluginEventManager.register(new FailListener(PluginEnabledEvent.class));
        manager.disablePluginModule(moduleKey);
        assertNotNull(getPluginAccessor().getPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getEnabledPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getPluginModule(moduleKey));
        assertNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
        assertTrue(getPluginAccessor().getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertTrue(getPluginAccessor().getEnabledModuleDescriptorsByType("animal").isEmpty());

        // now enable the module again
        pluginEventManager.register(new FailListener(PluginDisabledEvent.class));
        manager.enablePluginModule(moduleKey);
        assertNotNull(getPluginAccessor().getPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getEnabledPlugin(pluginKey));
        assertNotNull(getPluginAccessor().getPluginModule(moduleKey));
        assertNotNull(getPluginAccessor().getEnabledPluginModule(moduleKey));
        assertFalse(getPluginAccessor().getEnabledModulesByClass(com.atlassian.plugin.mock.MockBear.class).isEmpty());
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class).isEmpty());
        assertFalse(getPluginAccessor().getEnabledModuleDescriptorsByType("animal").isEmpty());
    }

    public void testDuplicatePluginKeysAreBad() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        try
        {
            manager.init();
            fail("Should have died with duplicate key exception.");
        }
        catch (final PluginParseException e)
        {
            assertEquals("Duplicate plugin found (installed version is the same or older) and could not be unloaded: 'test.disabled.plugin'",
                    e.getMessage());
        }
    }

    public void testLoadOlderDuplicatePlugin() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        pluginLoaders.add(new MultiplePluginLoader("test-atlassian-plugin-newer.xml"));
        pluginLoaders.add(new MultiplePluginLoader("test-atlassian-plugin.xml", "test-another-plugin.xml"));
        manager.init();
        assertEquals(2, getPluginAccessor().getEnabledPlugins().size());
    }

    public void testLoadOlderDuplicatePluginDoesNotTryToEnableIt()
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        pluginLoaders.add(new MultiplePluginLoader("test-atlassian-plugin-newer.xml"));
        final Plugin plugin = new StaticPlugin()
        {
            @Override
            protected PluginState enableInternal()
            {
                fail("enable() must never be called on a earlier version of plugin when later version is installed");
                return null;
            }

            @Override
            public void disableInternal()
            {
                fail("disable() must never be called on a earlier version of plugin when later version is installed");
            }
        };
        plugin.setKey("test.atlassian.plugin");
        plugin.getPluginInformation().setVersion("1.0");
        manager.init();
        manager.addPlugins(null, Collections.singletonList(plugin));
    }

    public void testLoadNewerDuplicatePlugin() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin-newer.xml"));
        try
        {
            manager.init();
            fail("Should have died with duplicate key exception.");
        }
        catch (final PluginParseException e)
        {
            assertEquals("Duplicate plugin found (installed version is the same or older) and could not be unloaded: 'test.atlassian.plugin'",
                    e.getMessage());
        }
    }

    public void testLoadNewerDuplicateDynamicPluginPreservesPluginState() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);

        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin.xml"));
        manager.init();

        pluginStateStore.save(PluginPersistentState.Builder.create(pluginStateStore.load()).setEnabled(manager.getPlugin("test.atlassian.plugin"),
                false).toState());

        assertFalse(getPluginAccessor().isPluginEnabled("test.atlassian.plugin"));
        manager.shutdown();

        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin-newer.xml"));
        manager.init();

        final Plugin plugin = getPluginAccessor().getPlugin("test.atlassian.plugin");
        assertEquals("1.1", plugin.getPluginInformation().getVersion());
        assertFalse(getPluginAccessor().isPluginEnabled("test.atlassian.plugin"));
    }

    public void testLoadNewerDuplicateDynamicPluginPreservesModuleState() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);

        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin.xml"));
        manager.init();

        pluginStateStore.save(PluginPersistentState.Builder.create(pluginStateStore.load()).setEnabled(
                getPluginAccessor().getPluginModule("test.atlassian.plugin:bear"), false).toState());

        manager.shutdown();

        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin-newer.xml"));
        manager.init();

        final Plugin plugin = getPluginAccessor().getPlugin("test.atlassian.plugin");
        assertEquals("1.1", plugin.getPluginInformation().getVersion());
        assertFalse(getPluginAccessor().isPluginModuleEnabled("test.atlassian.plugin:bear"));
        assertTrue(getPluginAccessor().isPluginModuleEnabled("test.atlassian.plugin:gold"));
    }

    public void testLoadChangedDynamicPluginWithSameVersionNumberReplacesExisting() throws PluginParseException
    {
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);

        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoaderWithRemoval("test-atlassian-plugin-changed-same-version.xml"));

        manager.init();

        final Plugin plugin = getPluginAccessor().getPlugin("test.atlassian.plugin");
        assertEquals("Test Plugin (Changed)", plugin.getName());
    }

    public void testGetPluginsWithPluginMatchingPluginPredicate() throws Exception
    {
        final Mock mockPlugin = mockTestPlugin(Collections.emptyList());
        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockPluginPredicate = new Mock(PluginPredicate.class);
        mockPluginPredicate.expectAndReturn("matches", C.eq(plugin), true);

        manager.addPlugins(null, Collections.singletonList(plugin));
        final Collection<Plugin> plugins = getPluginAccessor().getPlugins((PluginPredicate) mockPluginPredicate.proxy());

        assertEquals(1, plugins.size());
        assertTrue(plugins.contains(plugin));
        mockPluginPredicate.verify();
    }

    public void testGetPluginsWithPluginNotMatchingPluginPredicate() throws Exception
    {
        final Mock mockPlugin = mockTestPlugin(Collections.emptyList());
        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockPluginPredicate = new Mock(PluginPredicate.class);
        mockPluginPredicate.expectAndReturn("matches", C.eq(plugin), false);

        manager.addPlugins(null, Collections.singletonList(plugin));
        final Collection<Plugin> plugins = getPluginAccessor().getPlugins((PluginPredicate) mockPluginPredicate.proxy());

        assertEquals(0, plugins.size());
        mockPluginPredicate.verify();
    }

    public void testGetPluginModulesWithModuleMatchingPredicate() throws Exception
    {
        final MockThing module = new MockThing()
        {
        };
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<MockThing> moduleDescriptor = (ModuleDescriptor<MockThing>) mockModuleDescriptor.proxy();
        mockModuleDescriptor.expectAndReturn("getModule", module);
        mockModuleDescriptor.matchAndReturn("getCompleteKey", "some-plugin-key:module");
        mockModuleDescriptor.matchAndReturn("isEnabledByDefault", true);
        mockModuleDescriptor.matchAndReturn("hashCode", mockModuleDescriptor.hashCode());

        final Mock mockPlugin = mockTestPlugin(Collections.singleton(moduleDescriptor));
        mockPlugin.matchAndReturn("getModuleDescriptor", "module", moduleDescriptor);

        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockModulePredicate = new Mock(ModuleDescriptorPredicate.class);
        mockModulePredicate.expectAndReturn("matches", C.eq(moduleDescriptor), true);

        manager.addPlugins(null, Collections.singletonList(plugin));
        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<MockThing> predicate = (ModuleDescriptorPredicate<MockThing>) mockModulePredicate.proxy();
        final Collection<MockThing> modules = getPluginAccessor().getModules(predicate);

        assertEquals(1, modules.size());
        assertTrue(modules.contains(module));

        mockModulePredicate.verify();
    }

    public void testGetPluginModulesWithGetModuleThrowingException() throws Exception
    {
        final Plugin badPlugin = new StaticPlugin();
        badPlugin.setKey("bad");
        final MockModuleDescriptor<Object> badDescriptor = new MockModuleDescriptor<Object>(badPlugin, "bad", new Object())
        {
            @Override
            public Object getModule()
            {
                throw new RuntimeException();
            }
        };
        badPlugin.addModuleDescriptor(badDescriptor);

        final Plugin goodPlugin = new StaticPlugin();
        goodPlugin.setKey("good");
        final MockModuleDescriptor<Object> goodDescriptor = new MockModuleDescriptor<Object>(goodPlugin, "good", new Object());
        goodPlugin.addModuleDescriptor(goodDescriptor);

        manager.addPlugins(null, Arrays.asList(goodPlugin, badPlugin));
        manager.enablePlugin("bad");
        manager.enablePlugin("good");

        assertTrue(getPluginAccessor().isPluginEnabled("bad"));
        assertTrue(getPluginAccessor().isPluginEnabled("good"));
        final Collection<Object> modules = getPluginAccessor().getEnabledModulesByClass(Object.class);

        assertEquals(1, modules.size());
        assertFalse(getPluginAccessor().isPluginEnabled("bad"));
        assertTrue(getPluginAccessor().isPluginEnabled("good"));
    }

    public void testGetPluginModulesWithModuleNotMatchingPredicate() throws Exception
    {
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<MockThing> moduleDescriptor = (ModuleDescriptor<MockThing>) mockModuleDescriptor.proxy();
        mockModuleDescriptor.matchAndReturn("getCompleteKey", "some-plugin-key:module");
        mockModuleDescriptor.matchAndReturn("isEnabledByDefault", true);
        mockModuleDescriptor.matchAndReturn("hashCode", mockModuleDescriptor.hashCode());

        final Mock mockPlugin = mockTestPlugin(Collections.singleton(moduleDescriptor));
        mockPlugin.matchAndReturn("getModuleDescriptor", "module", moduleDescriptor);
        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockModulePredicate = new Mock(ModuleDescriptorPredicate.class);
        mockModulePredicate.expectAndReturn("matches", C.eq(moduleDescriptor), false);

        manager.addPlugins(null, Collections.singletonList(plugin));
        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<MockThing> predicate = (ModuleDescriptorPredicate<MockThing>) mockModulePredicate.proxy();
        final Collection<MockThing> modules = getPluginAccessor().getModules(predicate);

        assertEquals(0, modules.size());

        mockModulePredicate.verify();
    }

    public void testGetPluginModuleDescriptorWithModuleMatchingPredicate() throws Exception
    {
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<MockThing> moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
        mockModuleDescriptor.matchAndReturn("getCompleteKey", "some-plugin-key:module");
        mockModuleDescriptor.matchAndReturn("isEnabledByDefault", true);
        mockModuleDescriptor.matchAndReturn("hashCode", mockModuleDescriptor.hashCode());

        final Mock mockPlugin = mockTestPlugin(Collections.singleton(moduleDescriptor));
        mockPlugin.matchAndReturn("getModuleDescriptor", "module", moduleDescriptor);
        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockModulePredicate = new Mock(ModuleDescriptorPredicate.class);
        mockModulePredicate.expectAndReturn("matches", C.eq(moduleDescriptor), true);

        manager.addPlugins(null, Collections.singletonList(plugin));
        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<MockThing> predicate = (ModuleDescriptorPredicate<MockThing>) mockModulePredicate.proxy();
        final Collection<ModuleDescriptor<MockThing>> modules = getPluginAccessor().getModuleDescriptors(predicate);

        assertEquals(1, modules.size());
        assertTrue(modules.contains(moduleDescriptor));

        mockModulePredicate.verify();
    }

    public void testGetPluginModuleDescriptorsWithModuleNotMatchingPredicate() throws Exception
    {
        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<MockThing> moduleDescriptor = (ModuleDescriptor<MockThing>) mockModuleDescriptor.proxy();
        mockModuleDescriptor.matchAndReturn("getCompleteKey", "some-plugin-key:module");
        mockModuleDescriptor.matchAndReturn("isEnabledByDefault", true);
        mockModuleDescriptor.matchAndReturn("hashCode", mockModuleDescriptor.hashCode());

        final Mock mockPlugin = mockTestPlugin(Collections.singleton(moduleDescriptor));
        mockPlugin.matchAndReturn("getModuleDescriptor", "module", moduleDescriptor);
        final Plugin plugin = (Plugin) mockPlugin.proxy();

        final Mock mockModulePredicate = new Mock(ModuleDescriptorPredicate.class);
        mockModulePredicate.expectAndReturn("matches", C.eq(moduleDescriptor), false);

        manager.addPlugins(null, Collections.singletonList(plugin));
        @SuppressWarnings("unchecked")
        final ModuleDescriptorPredicate<MockThing> predicate = (ModuleDescriptorPredicate<MockThing>) mockModulePredicate.proxy();
        final Collection<MockThing> modules = getPluginAccessor().getModules(predicate);

        assertEquals(0, modules.size());

        mockModulePredicate.verify();
    }

    private Mock mockTestPlugin(Collection moduleDescriptors)
    {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", "some-plugin-key");
        mockPlugin.matchAndReturn("hashCode", 12);
        mockPlugin.expect("install");
        mockPlugin.expect("enable");
        mockPlugin.expectAndReturn("isSystemPlugin", false);
        mockPlugin.matchAndReturn("isEnabledByDefault", true);
        mockPlugin.matchAndReturn("isEnabled", true);
        mockPlugin.matchAndReturn("getPluginState", PluginState.ENABLED);
        mockPlugin.matchAndReturn("getModuleDescriptors", moduleDescriptors);
        return mockPlugin;
    }

    public void testGetPluginAndModules() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        final Plugin plugin = manager.getPlugin("test.atlassian.plugin");
        assertNotNull(plugin);
        assertEquals("Test Plugin", plugin.getName());

        final ModuleDescriptor<?> bear = plugin.getModuleDescriptor("bear");
        assertEquals(bear, getPluginAccessor().getPluginModule("test.atlassian.plugin:bear"));
    }

    public void testGetModuleByModuleClassOneFound() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        final List<MockAnimalModuleDescriptor> animalDescriptors = getPluginAccessor().getEnabledModuleDescriptorsByClass(MockAnimalModuleDescriptor.class);
        assertNotNull(animalDescriptors);
        assertEquals(1, animalDescriptors.size());
        final ModuleDescriptor<MockAnimal> moduleDescriptor = animalDescriptors.iterator().next();
        assertEquals("Bear Animal", moduleDescriptor.getName());

        final List<MockMineralModuleDescriptor> mineralDescriptors = getPluginAccessor().getEnabledModuleDescriptorsByClass(MockMineralModuleDescriptor.class);
        assertNotNull(mineralDescriptors);
        assertEquals(1, mineralDescriptors.size());
        final ModuleDescriptor<MockMineral> mineralDescriptor = mineralDescriptors.iterator().next();
        assertEquals("Bar", mineralDescriptor.getName());
    }

    public void testGetModuleByModuleClassAndDescriptor() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        final Collection<MockBear> bearModules = getPluginAccessor().getEnabledModulesByClassAndDescriptor(
                new Class[]{MockAnimalModuleDescriptor.class, MockMineralModuleDescriptor.class}, MockBear.class);
        assertNotNull(bearModules);
        assertEquals(1, bearModules.size());
        assertTrue(bearModules.iterator().next() instanceof MockBear);

        final Collection<MockBear> noModules = getPluginAccessor().getEnabledModulesByClassAndDescriptor(new Class[]{}, MockBear.class);
        assertNotNull(noModules);
        assertEquals(0, noModules.size());

        final Collection<MockThing> mockThings = getPluginAccessor().getEnabledModulesByClassAndDescriptor(
                new Class[]{MockAnimalModuleDescriptor.class, MockMineralModuleDescriptor.class}, MockThing.class);
        assertNotNull(mockThings);
        assertEquals(2, mockThings.size());
        assertTrue(mockThings.iterator().next() instanceof MockThing);
        assertTrue(mockThings.iterator().next() instanceof MockThing);

        final Collection<MockThing> mockThingsFromMineral = getPluginAccessor().getEnabledModulesByClassAndDescriptor(
                new Class[]{MockMineralModuleDescriptor.class}, MockThing.class);
        assertNotNull(mockThingsFromMineral);
        assertEquals(1, mockThingsFromMineral.size());
        final Object o = mockThingsFromMineral.iterator().next();
        assertTrue(o instanceof MockMineral);
    }

    public void testGetModuleByModuleClassNoneFound() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        class MockSilver implements MockMineral
        {
            public int getWeight()
            {
                return 3;
            }
        }

        final Collection<MockSilver> descriptors = getPluginAccessor().getEnabledModulesByClass(MockSilver.class);
        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());
    }

    public void testGetModuleDescriptorsByType() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        Collection<ModuleDescriptor<MockThing>> descriptors = getPluginAccessor().getEnabledModuleDescriptorsByType("animal");
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        ModuleDescriptor<MockThing> moduleDescriptor = descriptors.iterator().next();
        assertEquals("Bear Animal", moduleDescriptor.getName());

        descriptors = getPluginAccessor().getEnabledModuleDescriptorsByType("mineral");
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        moduleDescriptor = descriptors.iterator().next();
        assertEquals("Bar", moduleDescriptor.getName());

        try
        {
            getPluginAccessor().getEnabledModuleDescriptorsByType("foobar");
        }
        catch (final IllegalArgumentException e)
        {
            fail("Shouldn't have thrown exception.");
        }
    }

    public void testRetrievingDynamicResources() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        final InputStream is = manager.getPluginResourceAsStream("test.atlassian.plugin.classloaded", "atlassian-plugin.xml");
        assertNotNull(is);
        IOUtils.closeQuietly(is);
    }

    public void testGetDynamicPluginClass() throws IOException, PluginParseException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        try
        {
            manager.getDynamicPluginClass("com.atlassian.plugin.mock.MockPooh");
        }
        catch (final ClassNotFoundException e)
        {
            fail(e.getMessage());
        }
    }

    public void testFindingNewPlugins() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        //delete paddington for the timebeing
        final File paddington = new File(pluginsTestDir, PADDINGTON_JAR);
        paddington.delete();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

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

    public void testFindingNewPluginsNotLoadingRestartRequiredDescriptors() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        assertEquals(2, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));

        pluginLoaders.add(new DynamicSinglePluginLoader("test.atlassian.plugin", "test-requiresRestart-plugin.xml"));

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));
        assertNotNull(manager.getPlugin("test.atlassian.plugin"));

        final Plugin plugin = manager.getPlugin("test.atlassian.plugin");
        assertTrue(plugin instanceof UnloadablePlugin);
        assertTrue(((UnloadablePlugin)plugin).getErrorText().contains("foo"));

        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.atlassian.plugin"));
    }

    /**
     * Tests upgrade of plugin where the old version didn't have any restart required module descriptors, but the new one does
     */
    public void testFindingUpgradePluginsNotLoadingRestartRequiredDescriptors() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        assertEquals(2, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        pluginLoaders.add(new DynamicSinglePluginLoader("test.atlassian.plugin.classloaded2", "test-requiresRestartWithUpgrade-plugin.xml"));

        manager.scanForNewPlugins();
        assertEquals(2, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.atlassian.plugin.classloaded2"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.atlassian.plugin.classloaded2"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
    }

    public void testInstallPluginThatRequiresRestart() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);
        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        assertEquals(2, manager.getPlugins().size());

        new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' i18n-name-key='test.name' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);
        manager.scanForNewPlugins();

        assertEquals(3, manager.getPlugins().size());
        Plugin plugin = manager.getPlugin("test.restartrequired");
        assertNotNull(plugin);
        assertEquals("Test 2", plugin.getName());
        assertEquals("test.name", plugin.getI18nNameKey());
        assertEquals(1, plugin.getPluginsVersion());
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.restartrequired"));

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(plugin);
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testInstallPluginThatRequiresRestartThenRevert() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);
        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));
        assertEquals(2, manager.getPlugins().size());

        File pluginJar = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' i18n-name-key='test.name' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build();
        manager.installPlugin(new JarPluginArtifact(pluginJar));

        assertEquals(3, manager.getPlugins().size());
        Plugin plugin = manager.getPlugin("test.restartrequired");
        assertNotNull(plugin);
        assertEquals("Test 2", plugin.getName());
        assertEquals("test.name", plugin.getI18nNameKey());
        assertEquals(1, plugin.getPluginsVersion());
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.restartrequired"));

        manager.revertRestartRequiredChange("test.restartrequired");
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.shutdown();
        manager.init();

        assertEquals(2, manager.getPlugins().size());
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testUpgradePluginThatRequiresRestart() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "    <requiresRestart key='bar' />", "</atlassian-plugin>").build();

        origFile.delete();
        FileUtils.moveFile(updateFile, origFile);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(2, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testUpgradePluginThatRequiresRestartThenReverted() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "    <requiresRestart key='bar' />", "</atlassian-plugin>").build();

        manager.installPlugin(new JarPluginArtifact(updateFile));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.revertRestartRequiredChange("test.restartrequired");
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals("1.0", manager.getPlugin("test.restartrequired").getPluginInformation().getVersion());
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testUpgradePluginThatRequiresRestartThenRevertedRevertsToOriginalPlugin() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        // Add the plugin to the plugins test directory so it is included when we first start up
        new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "    <requiresRestart key='bar' />", "</atlassian-plugin>").build();

        // Install version 2 of the plugin
        manager.installPlugin(new JarPluginArtifact(updateFile));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        Thread.sleep(1000);
        final File updateFile2 = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>3.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "    <requiresRestart key='bar' />", "</atlassian-plugin>").build();

        // Install version 3 of the plugin
        manager.installPlugin(new JarPluginArtifact(updateFile2));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        // Lets revert the whole upgrade so that the original plugin is restored
        manager.revertRestartRequiredChange("test.restartrequired");
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals("1.0", manager.getPlugin("test.restartrequired").getPluginInformation().getVersion());

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals("1.0", manager.getPlugin("test.restartrequired").getPluginInformation().getVersion());
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }
    
    public void testUpgradePluginThatRequiresRestartMultipleTimeStaysUpgraded() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build();

        manager.installPlugin(new JarPluginArtifact(updateFile));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        Thread.sleep(1000);
        final File updateFile2 = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>3.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build();

        manager.installPlugin(new JarPluginArtifact(updateFile2));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testUpgradePluginThatPreviouslyRequiredRestart() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "</atlassian-plugin>").build(pluginsTestDir);

        origFile.delete();
        FileUtils.moveFile(updateFile, origFile);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.UPGRADE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }
    
    public void testInstallPluginThatPreviouslyRequiredRestart() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(2, manager.getPlugins().size());
        assertNull(manager.getPlugin("test.restartrequired"));

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "</atlassian-plugin>").build(pluginsTestDir);

        origFile.delete();
        FileUtils.moveFile(updateFile, origFile);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testInstallPluginMoreThanOnceStaysAsInstall() throws PluginParseException, IOException, InterruptedException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(2, manager.getPlugins().size());
        assertNull(manager.getPlugin("test.restartrequired"));
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        final File origFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.restartrequired"));

        // Some filesystems only record last modified in seconds
        Thread.sleep(1000);
        final File updateFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>2.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        origFile.delete();
        FileUtils.moveFile(updateFile, origFile);

        manager.scanForNewPlugins();
        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.INSTALL, manager.getPluginRestartState("test.restartrequired"));

        manager.shutdown();
        manager.init();

        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }
    
    public void testRemovePluginThatRequiresRestart() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File pluginFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.uninstall(manager.getPlugin("test.restartrequired"));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.REMOVE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertFalse(pluginFile.exists());
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(2, manager.getPlugins().size());
    }

    public void testRemovePluginThatRequiresRestartThenReverted() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        final File pluginFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.uninstall(manager.getPlugin("test.restartrequired"));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.REMOVE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());

        manager.revertRestartRequiredChange("test.restartrequired");
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.shutdown();
        manager.init();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
    }

    public void testRemovePluginThatRequiresRestartViaSubclass() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestartSubclass", RequiresRestartSubclassModuleDescriptor.class);

        final File pluginFile = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestartSubclass key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartSubclassModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.uninstall(manager.getPlugin("test.restartrequired"));

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.REMOVE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartSubclassModuleDescriptor.class).size());

        manager.shutdown();
        manager.init();

        assertFalse(pluginFile.exists());
        assertEquals(0, manager.getEnabledModuleDescriptorsByClass(RequiresRestartSubclassModuleDescriptor.class).size());
        assertEquals(2, manager.getPlugins().size());
    }

    public void testDisableEnableOfPluginThatRequiresRestart() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='1'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <requiresRestart key='foo' />", "</atlassian-plugin>").build(pluginsTestDir);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));

        manager.disablePlugin("test.restartrequired");
        assertFalse(manager.isPluginEnabled("test.restartrequired"));
        manager.enablePlugins("test.restartrequired");

        assertEquals(3, manager.getPlugins().size());
        assertNotNull(manager.getPlugin("test.restartrequired"));
        assertTrue(manager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.NONE, manager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, manager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
    }

    public void testCannotRemovePluginFromStaticLoader() throws PluginParseException, IOException
    {
        createFillAndCleanTempPluginDirectory();
        moduleDescriptorFactory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);

        directoryPluginLoader = new DirectoryPluginLoader(pluginsTestDir, Arrays.asList(new LegacyDynamicPluginFactory(
                PluginAccessor.Descriptor.FILENAME), new XmlDynamicPluginFactory("key")), pluginEventManager);
        pluginLoaders.add(directoryPluginLoader);
        pluginLoaders.add(new SinglePluginLoader("test-requiresRestart-plugin.xml"));

        manager.init();

        assertEquals(3, getPluginAccessor().getPlugins().size());
        assertNotNull(getPluginAccessor().getPlugin("test.atlassian.plugin"));
        assertTrue(getPluginAccessor().isPluginEnabled("test.atlassian.plugin"));
        assertEquals(1, getPluginAccessor().getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, getPluginAccessor().getPluginRestartState("test.atlassian.plugin"));

        try
        {
            manager.uninstall(getPluginAccessor().getPlugin("test.atlassian.plugin"));
            fail();
        }
        catch (final PluginException ex)
        {
            // test passed
        }

        assertEquals(3, getPluginAccessor().getPlugins().size());
        assertNotNull(getPluginAccessor().getPlugin("test.atlassian.plugin"));
        assertTrue(getPluginAccessor().isPluginEnabled("test.atlassian.plugin"));
        assertEquals(PluginRestartState.NONE, getPluginAccessor().getPluginRestartState("test.atlassian.plugin"));
        assertEquals(1, getPluginAccessor().getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
    }

    private DefaultPluginManager makeClassLoadingPluginManager() throws PluginParseException
    {
        directoryPluginLoader = new DirectoryPluginLoader(pluginsTestDir, Arrays.asList(new LegacyDynamicPluginFactory(
                PluginAccessor.Descriptor.FILENAME), new XmlDynamicPluginFactory("key")), pluginEventManager);
        pluginLoaders.add(directoryPluginLoader);

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        manager.init();
        return manager;
    }

    public void testRemovingPlugins() throws PluginException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        assertEquals(2, manager.getPlugins().size());
        final MockAnimalModuleDescriptor moduleDescriptor = (MockAnimalModuleDescriptor) manager.getPluginModule("test.atlassian.plugin.classloaded:paddington");
        assertFalse(moduleDescriptor.disabled);
        final PassListener disabledListener = new PassListener(PluginDisabledEvent.class);
        pluginEventManager.register(disabledListener);
        final Plugin plugin = manager.getPlugin("test.atlassian.plugin.classloaded");
        manager.uninstall(plugin);
        assertTrue("Module must have had disable() called before being removed", moduleDescriptor.disabled);

        // uninstalling a plugin should remove it's state completely from the state store - PLUG-13
        assertTrue(pluginStateStore.load().getPluginStateMap(plugin).isEmpty());

        assertEquals(1, manager.getPlugins().size());
        // plugin is no longer available though the plugin manager
        assertNull(manager.getPlugin("test.atlassian.plugin.classloaded"));
        assertEquals(1, pluginsTestDir.listFiles().length);
        disabledListener.assertCalled();
    }

    public void testPluginModuleAvailableAfterInstallation()
    {
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(pluginLoader.supportsRemoval()).thenReturn(true);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("dynPlugin");
        when(plugin.isEnabledByDefault()).thenReturn(true);
        when(plugin.isDeleteable()).thenReturn(true);
        when(plugin.isUninstallable()).thenReturn(true);
        when(plugin.getPluginState()).thenReturn(PluginState.ENABLED);
        when(plugin.compareTo(any(Plugin.class))).thenReturn(-1);
        when (pluginLoader.loadAllPlugins(any(ModuleDescriptorFactory.class))).thenReturn(asList(plugin));
        pluginLoaders.add(pluginLoader);
        manager.init();

        PluginModuleEnabledListener listener = new PluginModuleEnabledListener();
        pluginEventManager.register(listener);
        Collection<ModuleDescriptor<?>> mods = new ArrayList<ModuleDescriptor<?>>();
        MockModuleDescriptor<String> moduleDescriptor = new MockModuleDescriptor<String>(plugin, "foo", "foo");
        mods.add(moduleDescriptor);
        when(plugin.getModuleDescriptors()).thenReturn(mods);
        when(plugin.getModuleDescriptor("foo")).thenReturn((ModuleDescriptor)moduleDescriptor);
        pluginEventManager.broadcast(new PluginModuleAvailableEvent(moduleDescriptor));

        assertTrue(getPluginAccessor().isPluginModuleEnabled("dynPlugin:foo"));
        assertTrue(listener.called);
    }

    public void testPluginModuleAvailableAfterInstallationButConfiguredToBeDisabled()
    {
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(pluginLoader.supportsRemoval()).thenReturn(true);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("dynPlugin");
        when(plugin.isEnabledByDefault()).thenReturn(true);
        when(plugin.isDeleteable()).thenReturn(true);
        when(plugin.isUninstallable()).thenReturn(true);
        when(plugin.getPluginState()).thenReturn(PluginState.ENABLED);
        when(plugin.compareTo(any(Plugin.class))).thenReturn(-1);
        when (pluginLoader.loadAllPlugins(any(ModuleDescriptorFactory.class))).thenReturn(asList(plugin));
        pluginLoaders.add(pluginLoader);
        manager.init();

        MockModuleDescriptor<String> moduleDescriptor = new MockModuleDescriptor<String>(plugin, "foo", "foo");

        manager.disablePluginModuleState(moduleDescriptor, manager.getStore());

        PluginModuleEnabledListener listener = new PluginModuleEnabledListener();
        pluginEventManager.register(listener);
        Collection<ModuleDescriptor<?>> mods = new ArrayList<ModuleDescriptor<?>>();
        mods.add(moduleDescriptor);

        when(plugin.getModuleDescriptors()).thenReturn(mods);
        when(plugin.getModuleDescriptor("foo")).thenReturn((ModuleDescriptor)moduleDescriptor);
        pluginEventManager.broadcast(new PluginModuleAvailableEvent(moduleDescriptor));

        assertFalse(getPluginAccessor().isPluginModuleEnabled("dynPlugin:foo"));
        assertFalse(listener.called);
    }

    public void testPluginModuleUnavailableAfterInstallation()
    {
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(pluginLoader.supportsRemoval()).thenReturn(true);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("dynPlugin");
        when(plugin.isEnabledByDefault()).thenReturn(true);
        when(plugin.isDeleteable()).thenReturn(true);
        when(plugin.isUninstallable()).thenReturn(true);
        when(plugin.getPluginState()).thenReturn(PluginState.ENABLED);
        when(plugin.compareTo(any(Plugin.class))).thenReturn(-1);
        when (pluginLoader.loadAllPlugins(any(ModuleDescriptorFactory.class))).thenReturn(asList(plugin));
        pluginLoaders.add(pluginLoader);
        manager.init();

        PluginModuleDisabledListener listener = new PluginModuleDisabledListener();
        pluginEventManager.register(listener);
        Collection<ModuleDescriptor<?>> mods = new ArrayList<ModuleDescriptor<?>>();
        MockModuleDescriptor<String> moduleDescriptor = new MockModuleDescriptor<String>(plugin, "foo", "foo");
        mods.add(moduleDescriptor);
        when(plugin.getModuleDescriptors()).thenReturn(mods);
        when(plugin.getModuleDescriptor("foo")).thenReturn((ModuleDescriptor)moduleDescriptor);
        pluginEventManager.broadcast(new PluginModuleAvailableEvent(moduleDescriptor));
        assertTrue(getPluginAccessor().isPluginModuleEnabled("dynPlugin:foo"));
        assertFalse(listener.called);
        pluginEventManager.broadcast(new PluginModuleUnavailableEvent(moduleDescriptor));
        assertTrue(listener.called);
    }

    public void testPluginContainerUnavailable()
    {
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(pluginLoader.supportsRemoval()).thenReturn(true);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getKey()).thenReturn("dynPlugin");
        when(plugin.isEnabledByDefault()).thenReturn(true);
        when(plugin.isDeleteable()).thenReturn(true);
        when(plugin.isUninstallable()).thenReturn(true);
        when(plugin.getPluginState()).thenReturn(PluginState.ENABLED);
        when(plugin.compareTo(any(Plugin.class))).thenReturn(-1);
        Collection<ModuleDescriptor<?>> mods = new ArrayList<ModuleDescriptor<?>>();
        MockModuleDescriptor<String> moduleDescriptor = new MockModuleDescriptor<String>(plugin, "foo", "foo");
        mods.add(moduleDescriptor);
        when(plugin.getModuleDescriptors()).thenReturn(mods);
        when(plugin.getModuleDescriptor("foo")).thenReturn((ModuleDescriptor)moduleDescriptor);
        when (pluginLoader.loadAllPlugins(any(ModuleDescriptorFactory.class))).thenReturn(asList(plugin));
        pluginLoaders.add(pluginLoader);
        manager.init();

        PluginDisabledListener listener = new PluginDisabledListener();
        PluginModuleDisabledListener moduleDisabledListener = new PluginModuleDisabledListener();
        pluginEventManager.register(listener);
        pluginEventManager.register(moduleDisabledListener);
        when(plugin.getPluginState()).thenReturn(PluginState.DISABLED);
        pluginEventManager.broadcast(new PluginContainerUnavailableEvent("dynPlugin"));
        assertFalse(getPluginAccessor().isPluginEnabled("dynPlugin"));
        assertTrue(listener.called);
        assertFalse(moduleDisabledListener.called);
    }

    public void testUninstallPluginWithDependencies() throws PluginException, IOException
    {
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(pluginLoader.supportsRemoval()).thenReturn(true);
        Plugin child = mock(Plugin.class);
        when(child.getKey()).thenReturn("child");
        when(child.isEnabledByDefault()).thenReturn(true);
        when(child.getPluginState()).thenReturn(PluginState.ENABLED);
        when(child.getRequiredPlugins()).thenReturn(singleton("parent"));
        when(child.compareTo(any(Plugin.class))).thenReturn(-1);
        Plugin parent = mock(Plugin.class);
        when(parent.getKey()).thenReturn("parent");
        when(parent.isEnabledByDefault()).thenReturn(true);
        when(parent.isDeleteable()).thenReturn(true);
        when(parent.isUninstallable()).thenReturn(true);
        when(parent.getPluginState()).thenReturn(PluginState.ENABLED);
        when(parent.compareTo(any(Plugin.class))).thenReturn(-1);
        when (pluginLoader.loadAllPlugins(any(ModuleDescriptorFactory.class))).thenReturn(asList(child, parent));
        pluginLoaders.add(pluginLoader);
        manager.init();

        manager.uninstall(parent);
        verify(parent).enable();
        verify(parent).disable();
        verify(pluginLoader).removePlugin(parent);

        verify(child).enable();
        verify(child).disable();
    }

    public void testNonRemovablePlugins() throws PluginParseException
    {
        pluginLoaders.add(new SinglePluginLoader("test-atlassian-plugin.xml"));

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        manager.init();

        final Plugin plugin = getPluginAccessor().getPlugin("test.atlassian.plugin");
        assertFalse(plugin.isUninstallable());
        assertNotNull(plugin.getResourceAsStream("test-atlassian-plugin.xml"));

        try
        {
            manager.uninstall(plugin);
            fail("Where was the exception?");
        }
        catch (final PluginException p)
        {
        }
    }

    public void testNonDeletablePlugins() throws PluginException, IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        assertEquals(2, manager.getPlugins().size());

        // Set plugin file can't be deleted.
        final Plugin pluginToRemove = new AbstractDelegatingPlugin(manager.getPlugin("test.atlassian.plugin.classloaded"))
        {
            public boolean isDeleteable()
            {
                return false;
            }
        };

        // Disable plugin module before uninstall
        final MockAnimalModuleDescriptor moduleDescriptor = (MockAnimalModuleDescriptor) manager.getPluginModule("test.atlassian.plugin.classloaded:paddington");
        assertFalse(moduleDescriptor.disabled);

        manager.uninstall(pluginToRemove);

        assertTrue("Module must have had disable() called before being removed", moduleDescriptor.disabled);
        assertEquals(1, manager.getPlugins().size());
        assertNull(manager.getPlugin("test.atlassian.plugin.classloaded"));
        assertEquals(2, pluginsTestDir.listFiles().length);
    }

    // These methods test the plugin compareTo() function, which compares plugins based on their version numbers.

    public void testComparePluginNewer()
    {

        final Plugin p1 = createPluginWithVersion("1.1");
        final Plugin p2 = createPluginWithVersion("1.0");
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

    public void testComparePluginOlder()
    {
        final Plugin p1 = createPluginWithVersion("1.0");
        final Plugin p2 = createPluginWithVersion("1.1");
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

    public void testComparePluginEqual()
    {
        final Plugin p1 = createPluginWithVersion("1.0");
        final Plugin p2 = createPluginWithVersion("1.0");
        assertTrue(p1.compareTo(p2) == 0);

        p1.getPluginInformation().setVersion("1.1.0.0");
        p2.getPluginInformation().setVersion("1.1");
        assertTrue(p1.compareTo(p2) == 0);

        p1.getPluginInformation().setVersion(" 1 . 1 ");
        p2.getPluginInformation().setVersion("1.1");
        assertTrue(p1.compareTo(p2) == 0);
    }

    // If we can't understand the version of a plugin, then take the new one.

    public void testComparePluginNoVersion()
    {
        final Plugin p1 = createPluginWithVersion("1.0");
        final Plugin p2 = createPluginWithVersion("#$%");
        assertEquals(1, p1.compareTo(p2));

        p1.getPluginInformation().setVersion("#$%");
        p2.getPluginInformation().setVersion("1.0");
        assertEquals(-1, p1.compareTo(p2));
    }

    public void testComparePluginBadPlugin()
    {
        final Plugin p1 = createPluginWithVersion("1.0");
        final Plugin p2 = createPluginWithVersion("1.0");

        // Compare against something with a different key
        p2.setKey("bad.key");
        assertTrue(p1.compareTo(p2) != 0);
    }

    public void testInvalidationOfDynamicResourceCache() throws IOException, PluginException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

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

    public void testValidatePlugin() throws PluginParseException
    {
        final DefaultPluginManager manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory,
                new DefaultPluginEventManager());
        final Mock mockLoader = new Mock(DynamicPluginLoader.class);
        @SuppressWarnings("unchecked")
        final PluginLoader loader = (PluginLoader) mockLoader.proxy();
        pluginLoaders.add(loader);

        final Mock mockPluginJar = new Mock(PluginArtifact.class);
        final PluginArtifact pluginArtifact = (PluginArtifact) mockPluginJar.proxy();
        mockLoader.expectAndReturn("canLoad", C.args(C.eq(pluginArtifact)), "foo");

        final String key = manager.validatePlugin(pluginArtifact);
        assertEquals("foo", key);
        mockLoader.verify();
    }

    public void testValidatePluginWithNoDynamicLoaders() throws PluginParseException
    {
        final DefaultPluginManager manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory,
                new DefaultPluginEventManager());
        final Mock mockLoader = new Mock(PluginLoader.class);
        @SuppressWarnings("unchecked")
        final PluginLoader loader = (PluginLoader) mockLoader.proxy();
        pluginLoaders.add(loader);

        final Mock mockPluginJar = new Mock(PluginArtifact.class);
        final PluginArtifact pluginArtifact = (PluginArtifact) mockPluginJar.proxy();
        try
        {
            manager.validatePlugin(pluginArtifact);
            fail("Should have thrown exception");
        }
        catch (final IllegalStateException ex)
        {
            // test passed
        }
    }

    public void testInvalidationOfDynamicClassCache() throws IOException, PluginException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        checkClasses(manager, true);
        manager.disablePlugin("test.atlassian.plugin.classloaded");
        checkClasses(manager, false);
        manager.enablePlugin("test.atlassian.plugin.classloaded");
        checkClasses(manager, true);
        manager.uninstall(manager.getPlugin("test.atlassian.plugin.classloaded"));
        checkClasses(manager, false);
        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);
        manager.scanForNewPlugins();
        checkClasses(manager, true);
    }

    public void testInstallPlugin() throws Exception
    {
        final Mock mockPluginStateStore = new Mock(PluginPersistentStateStore.class);
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        final Mock mockPluginLoader = new Mock(DynamicPluginLoader.class);
        final Mock mockDescriptorParserFactory = new Mock(DescriptorParserFactory.class);
        final Mock mockDescriptorParser = new Mock(DescriptorParser.class);
        final Mock mockPluginJar = new Mock(PluginArtifact.class);
        final Mock mockRepository = new Mock(PluginInstaller.class);
        final Mock mockPlugin = new Mock(Plugin.class);

        final ModuleDescriptorFactory moduleDescriptorFactory = (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy();

        final DefaultPluginManager pluginManager = new DefaultPluginManager((PluginPersistentStateStore) mockPluginStateStore.proxy(),
                Collections.<PluginLoader>singletonList((PluginLoader) mockPluginLoader.proxy()), moduleDescriptorFactory, pluginEventManager);

        final Plugin plugin = (Plugin) mockPlugin.proxy();
        final PluginArtifact pluginArtifact = (PluginArtifact) mockPluginJar.proxy();

        mockPluginStateStore.expectAndReturn("load", new DefaultPluginPersistentState());
        mockPluginStateStore.expectAndReturn("load", new DefaultPluginPersistentState());
        mockPluginStateStore.expectAndReturn("load", new DefaultPluginPersistentState());
        mockPluginStateStore.expect("save", C.ANY_ARGS);
        mockDescriptorParser.matchAndReturn("getKey", "test");
        mockRepository.expect("installPlugin", C.args(C.eq("test"), C.eq(pluginArtifact)));
        mockPluginLoader.expectAndReturn("loadAllPlugins", C.eq(moduleDescriptorFactory), Collections.emptyList());
        mockPluginLoader.expectAndReturn("supportsAddition", true);
        mockPluginLoader.expectAndReturn("addFoundPlugins", moduleDescriptorFactory, Collections.singletonList(plugin));
        mockPluginLoader.expectAndReturn("canLoad", C.args(C.eq(pluginArtifact)), "test");
        mockPlugin.matchAndReturn("getKey", "test");
        mockPlugin.matchAndReturn("hashCode", mockPlugin.hashCode());
        mockPlugin.expectAndReturn("getModuleDescriptors", new ArrayList<Object>());
        mockPlugin.expectAndReturn("getModuleDescriptors", new ArrayList<Object>());
        mockPlugin.expectAndReturn("isEnabledByDefault", true);
        mockPlugin.expect("install");
        mockPlugin.expect("enable");
        mockPlugin.expectAndReturn("isSystemPlugin", false);
        mockPlugin.expectAndReturn("isEnabledByDefault", true);
        mockPlugin.matchAndReturn("isEnabled", true);
        mockPlugin.matchAndReturn("getPluginState", PluginState.ENABLED);

        pluginManager.setPluginInstaller((PluginInstaller) mockRepository.proxy());
        pluginManager.init();
        final PassListener enabledListener = new PassListener(PluginEnabledEvent.class);
        pluginEventManager.register(enabledListener);
        pluginManager.installPlugin(pluginArtifact);

        assertEquals(plugin, pluginManager.getPlugin("test"));
        assertTrue(pluginManager.isPluginEnabled("test"));

        mockPlugin.verify();
        mockRepository.verify();
        mockPluginJar.verify();
        mockDescriptorParser.verify();
        mockDescriptorParserFactory.verify();
        mockPluginLoader.verify();
        mockPluginStateStore.verify();
        enabledListener.assertCalled();
    }

    public void testInstallPluginsWithOne()
    {
        DynamicPluginLoader loader = mock(DynamicPluginLoader.class);
        ModuleDescriptorFactory descriptorFactory = mock(ModuleDescriptorFactory.class);
        PluginEventManager eventManager = mock(PluginEventManager.class);
        PluginInstaller installer = mock(PluginInstaller.class);
        DefaultPluginManager pm = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Collections.<PluginLoader>singletonList(loader), descriptorFactory, eventManager);
        pm.setPluginInstaller(installer);
        PluginArtifact artifact = mock(PluginArtifact.class);
        Plugin plugin = mock(Plugin.class);
        when(loader.canLoad(artifact)).thenReturn("foo");
        when(loader.addFoundPlugins(descriptorFactory)).thenReturn(Arrays.asList(plugin));

        pm.installPlugins(artifact);

        verify(loader).canLoad(artifact);
        verify(installer).installPlugin("foo", artifact);
    }

    public void testInstallPluginsWithTwo()
    {
        DynamicPluginLoader loader = mock(DynamicPluginLoader.class);
        ModuleDescriptorFactory descriptorFactory = mock(ModuleDescriptorFactory.class);
        PluginEventManager eventManager = mock(PluginEventManager.class);
        PluginInstaller installer = mock(PluginInstaller.class);
        DefaultPluginManager pm = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Collections.<PluginLoader>singletonList(loader), descriptorFactory, eventManager);
        pm.setPluginInstaller(installer);
        PluginArtifact artifactA = mock(PluginArtifact.class);
        Plugin pluginA = mock(Plugin.class);
        when(loader.canLoad(artifactA)).thenReturn("a");
        PluginArtifact artifactB = mock(PluginArtifact.class);
        Plugin pluginB = mock(Plugin.class);
        when(loader.canLoad(artifactB)).thenReturn("b");

        when(loader.addFoundPlugins(descriptorFactory)).thenReturn(Arrays.asList(pluginA, pluginB));

        pm.installPlugins(artifactA, artifactB);

        verify(loader).canLoad(artifactA);
        verify(loader).canLoad(artifactB);
        verify(installer).installPlugin("a", artifactA);
        verify(installer).installPlugin("b", artifactB);
    }

    public void testInstallPluginsWithTwoButOneFailsValidation()
    {
        DynamicPluginLoader loader = mock(DynamicPluginLoader.class);
        ModuleDescriptorFactory descriptorFactory = mock(ModuleDescriptorFactory.class);
        PluginEventManager eventManager = mock(PluginEventManager.class);
        PluginInstaller installer = mock(PluginInstaller.class);
        DefaultPluginManager pm = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Collections.<PluginLoader>singletonList(loader), descriptorFactory, eventManager);
        pm.setPluginInstaller(installer);
        PluginArtifact artifactA = mock(PluginArtifact.class);
        Plugin pluginA = mock(Plugin.class);
        when(loader.canLoad(artifactA)).thenReturn("a");
        PluginArtifact artifactB = mock(PluginArtifact.class);
        Plugin pluginB = mock(Plugin.class);
        when(loader.canLoad(artifactB)).thenReturn(null);

        when(loader.addFoundPlugins(descriptorFactory)).thenReturn(Arrays.asList(pluginA, pluginB));

        try
        {
            pm.installPlugins(artifactA, artifactB);
            fail("Should have not installed plugins");
        }
        catch (PluginParseException ex)
        {
            // this is good
        }

        verify(loader).canLoad(artifactA);
        verify(loader).canLoad(artifactB);
        verify(installer, never()).installPlugin("a", artifactA);
        verify(installer, never()).installPlugin("b", artifactB);
    }

    public void testInstallPluginsWithTwoButOneFailsValidationWithException()
    {
        DynamicPluginLoader loader = mock(DynamicPluginLoader.class);
        ModuleDescriptorFactory descriptorFactory = mock(ModuleDescriptorFactory.class);
        PluginEventManager eventManager = mock(PluginEventManager.class);
        PluginInstaller installer = mock(PluginInstaller.class);
        DefaultPluginManager pm = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Collections.<PluginLoader>singletonList(loader), descriptorFactory, eventManager);
        pm.setPluginInstaller(installer);
        PluginArtifact artifactA = mock(PluginArtifact.class);
        Plugin pluginA = mock(Plugin.class);
        when(loader.canLoad(artifactA)).thenReturn("a");
        PluginArtifact artifactB = mock(PluginArtifact.class);
        Plugin pluginB = mock(Plugin.class);
        doThrow(new PluginParseException()).when(loader).canLoad(artifactB);

        when(loader.addFoundPlugins(descriptorFactory)).thenReturn(Arrays.asList(pluginA, pluginB));

        try
        {
            pm.installPlugins(artifactA, artifactB);
            fail("Should have not installed plugins");
        }
        catch (PluginParseException ex)
        {
            // this is good
        }

        verify(loader).canLoad(artifactA);
        verify(loader).canLoad(artifactB);
        verify(installer, never()).installPlugin("a", artifactA);
        verify(installer, never()).installPlugin("b", artifactB);
    }


    private <T> void checkResources(final PluginAccessor manager, final boolean canGetGlobal, final boolean canGetModule) throws IOException
    {
        InputStream is = manager.getDynamicResourceAsStream("icon.gif");
        assertEquals(canGetGlobal, is != null);
        IOUtils.closeQuietly(is);
        is = manager.getDynamicResourceAsStream("bear/paddington.vm");
        assertEquals(canGetModule, is != null);
        IOUtils.closeQuietly(is);
    }

    private <T> void checkClasses(final PluginAccessor manager, final boolean canGet)
    {
        try
        {
            manager.getDynamicPluginClass("com.atlassian.plugin.mock.MockPaddington");
            if (!canGet)
            {
                fail("Class in plugin was successfully loaded");
            }
        }
        catch (final ClassNotFoundException e)
        {
            if (canGet)
            {
                fail(e.getMessage());
            }
        }
    }

    public void testAddPluginsThatThrowExceptionOnEnabled() throws Exception
    {
        final Plugin plugin = new CannotEnablePlugin();

        manager.addPlugins(null, Arrays.asList(plugin));

        assertFalse(plugin.getPluginState() == PluginState.ENABLED);
    }

    public void testUninstallPluginClearsState() throws IOException
    {
        createFillAndCleanTempPluginDirectory();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();

        checkClasses(manager, true);
        final Plugin plugin = manager.getPlugin("test.atlassian.plugin.classloaded");

        final ModuleDescriptor<?> module = plugin.getModuleDescriptor("paddington");
        assertTrue(manager.isPluginModuleEnabled(module.getCompleteKey()));
        manager.disablePluginModule(module.getCompleteKey());
        assertFalse(manager.isPluginModuleEnabled(module.getCompleteKey()));
        manager.uninstall(plugin);
        assertFalse(manager.isPluginModuleEnabled(module.getCompleteKey()));
        assertTrue(pluginStateStore.load().getPluginStateMap(plugin).isEmpty());
    }

    public void testCannotInitTwice() throws PluginParseException
    {
        manager.init();
        try
        {
            manager.init();
            fail("IllegalStateException expected");
        }
        catch (final IllegalStateException expected)
        {
        }
    }

    public void testCannotShutdownTwice() throws PluginParseException
    {
        manager.init();
        manager.shutdown();
        try
        {
            manager.shutdown();
            fail("IllegalStateException expected");
        }
        catch (final IllegalStateException expected)
        {
        }
    }

    public void testGetPluginWithNullKey()
    {
        manager.init();
        try
        {
            getPluginAccessor().getPlugin(null);
            fail();
        }
        catch (IllegalArgumentException ex)
        {
            // test passed
        }
    }

    public void testShutdownHandlesException()
    {
        final ThingsAreWrongListener listener = new ThingsAreWrongListener();
        pluginEventManager.register(listener);
        manager.init();
        try
        {
            //this should not throw an exception
            manager.shutdown();
        }
        catch (Exception e)
        {
            fail("Should not have thrown an exception!");
        }
        assertTrue(listener.isCalled());
    }

    private void writeToFile(String file, String line) throws IOException
    {
        final String resourceName = ClasspathFilePluginMetadata.class.getPackage().getName().replace(".", "/") + "/" + file;
        URL resource = getClass().getClassLoader().getResource(resourceName);

        FileOutputStream fout = new FileOutputStream(resource.getFile(), false);
        fout.write(line.getBytes(), 0, line.length());
        fout.close();

    }

    public void testExceptionOnRequiredPluginNotEnabling() throws Exception
    {
        try
        {
            writeToFile("application-required-modules.txt", "foo.required:bar");
            writeToFile("application-required-plugins.txt", "foo.required");

            final Mock mockPluginLoader = new Mock(PluginLoader.class);
            final ModuleDescriptor<Object> badModuleDescriptor = new AbstractModuleDescriptor<Object>(ModuleFactory.LEGACY_MODULE_FACTORY)
            {
                @Override
                public String getKey()
                {
                    return "bar";
                }

                @Override
                public String getCompleteKey()
                {
                    return "foo.required:bar";
                }

                @Override
                public void enabled()
                {
                    throw new IllegalArgumentException("Cannot enable");
                }

                @Override
                public Object getModule()
                {
                    return null;
                }
            };

            final AbstractModuleDescriptor goodModuleDescriptor = mock(AbstractModuleDescriptor.class);
            when(goodModuleDescriptor.getKey()).thenReturn("baz");
            when(goodModuleDescriptor.getCompleteKey()).thenReturn("foo.required:baz");

            Plugin plugin = new StaticPlugin()
            {
                @Override
                public Collection<ModuleDescriptor<?>> getModuleDescriptors()
                {
                    return Arrays.<ModuleDescriptor<?>>asList(goodModuleDescriptor, badModuleDescriptor);
                }

                @Override
                public ModuleDescriptor<Object> getModuleDescriptor(final String key)
                {
                    return badModuleDescriptor;
                }
            };
            plugin.setKey("foo.required");
            plugin.setEnabledByDefault(true);
            plugin.setPluginInformation(new PluginInformation());

            mockPluginLoader.expectAndReturn("loadAllPlugins", C.ANY_ARGS, Collections.singletonList(plugin));

            @SuppressWarnings("unchecked")
            final PluginLoader loader = (PluginLoader) mockPluginLoader.proxy();
            pluginLoaders.add(loader);

            try
            {
                manager.init();
            }
            catch(PluginException e)
            {
                // expected
                assertEquals("Unable to validate required plugins or modules", e.getMessage());
                return;
            }
            fail("A PluginException is expected when trying to initialize the plugins system with required plugins that do not load.");
        }
        finally
        {
            // remove references from required files
            writeToFile("application-required-modules.txt", "");
            writeToFile("application-required-plugins.txt", "");
        }
    }

    public static class ThingsAreWrongListener
    {
        private volatile boolean called = false;

        @PluginEventListener
        public void onFrameworkShutdown(final PluginFrameworkShutdownEvent event)
        {
            called = true;
            throw new NullPointerException("AAAH!");
        }

        public boolean isCalled()
        {
            return called;
        }
    }

    public Plugin createPluginWithVersion(final String version)
    {
        final Plugin p = new StaticPlugin();
        p.setKey("test.default.plugin");
        final PluginInformation pInfo = p.getPluginInformation();
        pInfo.setVersion(version);
        return p;
    }

    /**
     * Dummy plugin loader that reports that removal is supported and returns plugins that report that they can
     * be uninstalled.
     */
    private static class SinglePluginLoaderWithRemoval extends SinglePluginLoader
    {
        public SinglePluginLoaderWithRemoval(final String resource)
        {
            super(resource);
        }

        public boolean supportsRemoval()
        {

            return true;
        }

        public void removePlugin(final Plugin plugin) throws PluginException
        {
            plugins = Collections.emptyList();
        }

        protected StaticPlugin getNewPlugin()
        {
            return new StaticPlugin()
            {
                public boolean isUninstallable()
                {
                    return true;
                }
            };
        }
    }

    class NothingModuleDescriptor extends MockUnusedModuleDescriptor
    {
    }

    @RequiresRestart
    public static class RequiresRestartModuleDescriptor extends MockUnusedModuleDescriptor
    {
    }

    // A subclass of a module descriptor that @RequiresRestart; should inherit the annotation
    public static class RequiresRestartSubclassModuleDescriptor extends RequiresRestartModuleDescriptor
    {
        
    }

    private class MultiplePluginLoader implements PluginLoader
    {
        private final String[] descriptorPaths;

        public MultiplePluginLoader(final String... descriptorPaths)
        {
            this.descriptorPaths = descriptorPaths;
        }

        public Collection<Plugin> loadAllPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
        {
            final List<Plugin> result = new ArrayList<Plugin>(descriptorPaths.length);
            for (final String path : descriptorPaths)
            {
                final SinglePluginLoader loader = new SinglePluginLoader(path);
                result.addAll(loader.loadAllPlugins(moduleDescriptorFactory));
            }
            return result;
        }

        public boolean supportsAddition()
        {
            return false;
        }

        public boolean supportsRemoval()
        {
            return false;
        }

        public Collection<Plugin> addFoundPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
        {
            throw new UnsupportedOperationException("This PluginLoader does not support addition.");
        }

        public void removePlugin(final Plugin plugin) throws PluginException
        {
            throw new UnsupportedOperationException("This PluginLoader does not support addition.");
        }
    }

    private static class DynamicSinglePluginLoader extends SinglePluginLoader implements DynamicPluginLoader
    {
        private final String key;

        public DynamicSinglePluginLoader(final String key, final String resource)
        {
            super(resource);
            this.key = key;
        }

        public String canLoad(final PluginArtifact pluginArtifact) throws PluginParseException
        {
            return key;
        }

        public boolean supportsAddition()
        {
            return true;
        }

        @Override
        public Collection<Plugin> addFoundPlugins(final ModuleDescriptorFactory moduleDescriptorFactory)
        {
            return super.loadAllPlugins(moduleDescriptorFactory);
        }
    }

    private static class CannotEnablePlugin extends StaticPlugin
    {
        public CannotEnablePlugin()
        {
            setKey("foo");
        }

        @Override
        protected PluginState enableInternal()
        {
            throw new RuntimeException("boo");
        }

        public void disabled()
        {
        }
    }

    public static class PluginModuleEnabledListener
    {
        public volatile boolean called;
        @PluginEventListener
        public void onEnable(PluginModuleEnabledEvent event)
        {
            called = true;
        }
    }

    public static class PluginModuleDisabledListener
    {
        public volatile boolean called;
        @PluginEventListener
        public void onDisable(PluginModuleDisabledEvent event)
        {
            called = true;
        }
    }

    public static class PluginDisabledListener
    {
        public volatile boolean called;
        @PluginEventListener
        public void onDisable(PluginDisabledEvent event)
        {
            called = true;
        }
    }
}
