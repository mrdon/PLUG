package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockBear;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.net.URL;
import java.net.URISyntaxException;

public class TestDirectoryPluginLoader extends AbstractTestClassLoader
{
    private PluginEventManager pluginEventManager;
    private DirectoryPluginLoader loader;
    private DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private static final List DEFAULT_PLUGIN_FACTORIES =
            Collections.singletonList(new LegacyDynamicPluginFactory(DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME));

    public static final String BAD_PLUGIN_JAR = "bad-plugins/crap-plugin.jar";

    public void setUp() throws Exception
    {
        super.setUp();
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        pluginEventManager = new DefaultPluginEventManager();
        createFillAndCleanTempPluginDirectory();
    }

    public void tearDown() throws Exception
    {
        if (loader != null)
        {
            loader.shutDown();
        }
        FileUtils.deleteDirectory(pluginsTestDir);
        super.tearDown();
    }

    public void testAtlassianPlugin() throws Exception
    {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(2, plugins.size());

        for (Iterator iterator = plugins.iterator(); iterator.hasNext();)
        {
            Plugin plugin = (Plugin) iterator.next();

            assertTrue(plugin.getName().equals("Test Class Loaded Plugin") || plugin.getName().equals("Test Class Loaded Plugin 2"));

            if (plugin.getName().equals("Test Class Loaded Plugin")) // asserts for first plugin
            {
                assertEquals("Test Class Loaded Plugin", plugin.getName());
                assertEquals("test.atlassian.plugin.classloaded", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                MockAnimalModuleDescriptor paddingtonDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("paddington");
                assertEquals("Paddington Bear", paddingtonDescriptor.getName());
                MockBear paddington = (MockBear) paddingtonDescriptor.getModule();
                assertEquals("com.atlassian.plugin.mock.MockPaddington", paddington.getClass().getName());
            }
            else if (plugin.getName().equals("Test Class Loaded Plugin 2")) // asserts for second plugin
            {
                assertEquals("Test Class Loaded Plugin 2", plugin.getName());
                assertEquals("test.atlassian.plugin.classloaded2", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                MockAnimalModuleDescriptor poohDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("pooh");
                assertEquals("Pooh Bear", poohDescriptor.getName());
                MockBear pooh = (MockBear) poohDescriptor.getModule();
                assertEquals("com.atlassian.plugin.mock.MockPooh", pooh.getClass().getName());
            }
            else
            {
                fail("What plugin name?!");
            }
        }
    }

    // Tests that an UnloadablePlugin is returned when there's a missing class dependency
    public void testAtlassianPluginWithMissingClass() throws Exception
    {
        addTestModuleDecriptors();

        URL url = ClassLoaderUtils.getResource(BAD_PLUGIN_JAR, TestClassPathPluginLoader.class);
        String path = url.toExternalForm();
        path = path.replace('/', File.separatorChar);
        path = path.substring(5);
        File pluginFile = new File(path);
        loader = new DirectoryPluginLoader(pluginFile.getParentFile(), DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        Object[] pluginsArray = plugins.toArray();

        Plugin plugin = (Plugin) pluginsArray[0];

        assertEquals(UnloadablePlugin.class, plugin.getClass());

        // grab module descriptors
        Collection moduleDescriptors = plugin.getModuleDescriptors();
        Object[] descriptorsArray = moduleDescriptors.toArray();

        assertEquals(1, moduleDescriptors.size());

        ModuleDescriptor descriptor = (ModuleDescriptor) descriptorsArray[0];

        assertEquals(UnloadableModuleDescriptor.class, descriptor.getClass());
    }

    private void addTestModuleDecriptors()
    {
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
    }

    public void testSupportsAdditionAndRemoval()
    {
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        assertTrue(loader.supportsAddition());
        assertTrue(loader.supportsRemoval());
    }

    public void testNoFoundPlugins() throws PluginParseException
    {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        Collection col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertFalse(col.isEmpty());

        col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertTrue(col.isEmpty());
    }

    public void testFoundPlugin() throws PluginParseException, IOException
    {
        //delete paddington for the timebeing
        File paddington = new File(pluginsTestDir + File.separator + PADDINGTON_JAR);
        paddington.delete();

        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        loader.loadAllPlugins(moduleDescriptorFactory);

        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);

        Collection col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(1, col.size());
        // next time we shouldn't find any new plugins
        col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(0, col.size());
    }

    public void testRemovePlugin() throws PluginException, IOException
    {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        //duplicate the paddington plugin before removing the original
        //the duplicate will be used to restore the deleted original after the test
        File paddington = new File(pluginsTestDir + File.separator + PADDINGTON_JAR);

        Iterator iter = plugins.iterator();

        Plugin paddingtonPlugin = null;

        while (iter.hasNext())
        {
            Plugin plugin = (Plugin) iter.next();

            if (plugin.getName().equals("Test Class Loaded Plugin"))
            {
                paddingtonPlugin = plugin;
                break;
            }
        }

        if (paddingtonPlugin == null)
            fail("Can't find test plugin 1 (paddington)");

        loader.removePlugin(paddingtonPlugin);
    }

    public void testInvalidPluginHandled() throws IOException, PluginParseException
    {
        File atlassianPluginXML = new File(pluginsTestDir, "atlassian-plugin.xml");

        FileWriter writer = new FileWriter(atlassianPluginXML);
        writer.write("this is invalid XML - the classloadingpluginloader should throw PluginParseException");
        writer.close();

        //now jar up the evilplugin

        createJarFile("evilplugin.jar", atlassianPluginXML.getName(), pluginsTestDir.getAbsolutePath());

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals("evil jar wasn't loaded, but other plugins were", pluginsTestDir.list(new FilenameFilter(){

            public boolean accept(File directory, String fileName)
            {
                return fileName.endsWith(".jar");
            }
        }).length - 1, plugins.size());
    }

    public void testInstallPluginTwice() throws URISyntaxException, IOException, PluginParseException, InterruptedException
    {
        FileUtils.cleanDirectory(pluginsTestDir);
        File plugin = new File(pluginsTestDir, "some-plugin.jar");
        new PluginBuilder("plugin")
                .addPluginInformation("some.key", "My name", "1.0", 1)
                .addResource("foo.txt", "foo")
                .build()
                .renameTo(plugin);

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNotNull(((Plugin)plugins.iterator().next()).getResource("foo.txt"));
        assertNull(((Plugin)plugins.iterator().next()).getResource("bar.txt"));

        // sleep to ensure the new plugin is picked up
        Thread.currentThread().sleep(1000);
        
        new PluginBuilder("plugin")
                .addPluginInformation("some.key", "My name", "1.0", 1)
                .addResource("bar.txt", "bar")
                .build()
                .renameTo(plugin);
        plugins = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNull(((Plugin)plugins.iterator().next()).getResource("foo.txt"));
        assertNotNull(((Plugin)plugins.iterator().next()).getResource("bar.txt"));

    }

    private void createJarFile(String jarname, String jarEntry, String saveDir)
            throws IOException
    {
        OutputStream os = new FileOutputStream(saveDir + File.separator + jarname);
        JarOutputStream plugin1 = new JarOutputStream(os);
        JarEntry jarEntry1 = new JarEntry(jarEntry);

        plugin1.putNextEntry(jarEntry1);
        plugin1.closeEntry();
        plugin1.flush();
        plugin1.close();
    }

}
