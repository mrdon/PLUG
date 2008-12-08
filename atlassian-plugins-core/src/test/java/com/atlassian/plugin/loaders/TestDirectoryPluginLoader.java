package com.atlassian.plugin.loaders;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.factories.XmlDynamicPluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockBear;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class TestDirectoryPluginLoader extends AbstractTestClassLoader
{
    private PluginEventManager pluginEventManager;
    private DirectoryPluginLoader loader;
    private DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private static final List DEFAULT_PLUGIN_FACTORIES = Arrays.asList(new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME),
        new XmlDynamicPluginFactory());

    public static final String BAD_PLUGIN_JAR = "bad-plugins/crap-plugin.jar";

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        pluginEventManager = new DefaultPluginEventManager();
        createFillAndCleanTempPluginDirectory();
    }

    @Override
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
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(2, plugins.size());

        for (final Iterator iterator = plugins.iterator(); iterator.hasNext();)
        {
            final Plugin plugin = (Plugin) iterator.next();

            assertTrue(plugin.getName().equals("Test Class Loaded Plugin") || plugin.getName().equals("Test Class Loaded Plugin 2"));

            if (plugin.getName().equals("Test Class Loaded Plugin")) // asserts for first plugin
            {
                assertEquals("Test Class Loaded Plugin", plugin.getName());
                assertEquals("test.atlassian.plugin.classloaded", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                final MockAnimalModuleDescriptor paddingtonDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("paddington");
                paddingtonDescriptor.enabled();
                assertEquals("Paddington Bear", paddingtonDescriptor.getName());
                final MockBear paddington = (MockBear) paddingtonDescriptor.getModule();
                assertEquals("com.atlassian.plugin.mock.MockPaddington", paddington.getClass().getName());
            }
            else if (plugin.getName().equals("Test Class Loaded Plugin 2")) // asserts for second plugin
            {
                assertEquals("Test Class Loaded Plugin 2", plugin.getName());
                assertEquals("test.atlassian.plugin.classloaded2", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                final MockAnimalModuleDescriptor poohDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("pooh");
                poohDescriptor.enabled();
                assertEquals("Pooh Bear", poohDescriptor.getName());
                final MockBear pooh = (MockBear) poohDescriptor.getModule();
                assertEquals("com.atlassian.plugin.mock.MockPooh", pooh.getClass().getName());
            }
            else
            {
                fail("What plugin name?!");
            }
        }
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
        final File paddington = new File(pluginsTestDir + File.separator + PADDINGTON_JAR);
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
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        //duplicate the paddington plugin before removing the original
        //the duplicate will be used to restore the deleted original after the test
        final File paddington = new File(pluginsTestDir + File.separator + PADDINGTON_JAR);

        final Iterator iter = plugins.iterator();

        Plugin paddingtonPlugin = null;

        while (iter.hasNext())
        {
            final Plugin plugin = (Plugin) iter.next();

            if (plugin.getName().equals("Test Class Loaded Plugin"))
            {
                paddingtonPlugin = plugin;
                break;
            }
        }

        if (paddingtonPlugin == null)
        {
            fail("Can't find test plugin 1 (paddington)");
        }

        loader.removePlugin(paddingtonPlugin);
    }

    public void testInvalidPluginHandled() throws IOException, PluginParseException
    {
        createJarFile("evilplugin.jar", PluginAccessor.Descriptor.FILENAME, pluginsTestDir.getAbsolutePath());

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals("evil jar wasn't loaded, but other plugins were", pluginsTestDir.list(new FilenameFilter()
        {

            public boolean accept(final File directory, final String fileName)
            {
                return fileName.endsWith(".jar");
            }
        }).length - 1, plugins.size());
    }

    public void testInstallPluginTwice() throws URISyntaxException, IOException, PluginParseException, InterruptedException
    {
        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin = new File(pluginsTestDir, "some-plugin.jar");
        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("foo.txt", "foo").build().renameTo(plugin);

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNotNull(((Plugin) plugins.iterator().next()).getResource("foo.txt"));
        assertNull(((Plugin) plugins.iterator().next()).getResource("bar.txt"));

        Thread.currentThread();
        // sleep to ensure the new plugin is picked up
        Thread.sleep(1000);

        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("bar.txt", "bar").build().renameTo(plugin);
        plugins = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNull(((Plugin) plugins.iterator().next()).getResource("foo.txt"));
        assertNotNull(((Plugin) plugins.iterator().next()).getResource("bar.txt"));
        assertTrue(plugin.exists());

    }

    public void testMixedFactories() throws URISyntaxException, IOException, PluginParseException, InterruptedException
    {
        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin = new File(pluginsTestDir, "some-plugin.jar");
        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("foo.txt", "foo").build().renameTo(plugin);
        FileUtils.writeStringToFile(new File(pluginsTestDir, "foo.xml"), "<atlassian-plugin key=\"jim\"></atlassian-plugin>");

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(2, plugins.size());
    }

    public void testUnknownPluginArtifact() throws URISyntaxException, IOException, PluginParseException, InterruptedException
    {
        FileUtils.cleanDirectory(pluginsTestDir);
        FileUtils.writeStringToFile(new File(pluginsTestDir, "foo.bob"), "<an>");

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertTrue(plugins.iterator().next() instanceof UnloadablePlugin);
    }

    private void createJarFile(final String jarname, final String jarEntry, final String saveDir) throws IOException
    {
        final OutputStream os = new FileOutputStream(saveDir + File.separator + jarname);
        final JarOutputStream plugin1 = new JarOutputStream(os);
        final JarEntry jarEntry1 = new JarEntry(jarEntry);

        plugin1.putNextEntry(jarEntry1);
        plugin1.closeEntry();
        plugin1.flush();
        plugin1.close();
    }

}
