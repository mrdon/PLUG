package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.util.FileUtils;
import com.atlassian.plugin.loaders.classloading.AbstractTestClassLoader;
import com.atlassian.plugin.mock.MockAnimalModuleDescriptor;
import com.atlassian.plugin.mock.MockBear;
import com.atlassian.plugin.mock.MockMineralModuleDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestClassLoadingPluginLoader extends AbstractTestClassLoader
{
    public static final String PADDINGTON_JAR = "paddington-test-plugin.jar";
    public static final String POOH_JAR = "pooh-test-plugin.jar";

    private static final Log log = LogFactory.getLog(TestClassLoadingPluginLoader.class);
    ClassLoadingPluginLoader loader;
    File pluginsDirectory;
    DefaultModuleDescriptorFactory moduleDescriptorFactory;
    File tempDir;
    File pluginsTestDir;

    public void setUp() throws Exception
    {
        super.setUp();
        pluginsDirectory = getPluginsDirectory(); // hacky way of getting to the directoryPluginLoaderFiles classloading
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory();
        tempDir = new File(System.getProperty("java.io.tmpdir"));
        addTestModuleDecriptors();

        pluginsTestDir = new File(tempDir.toString() + File.separator +  "plugins");

        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);
        loader = new ClassLoadingPluginLoader(pluginsTestDir);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();

        FileUtils.deleteDir(pluginsTestDir);
    }


    public void testAtlassianPlugin() throws Exception
    {
        addTestModuleDecriptors();
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
                MockBear paddington = (MockBear)paddingtonDescriptor.getModule();
                assertEquals("com.atlassian.plugin.mock.MockPaddington", paddington.getClass().getName());
            }
            else if (plugin.getName().equals("Test Class Loaded Plugin 2")) // asserts for second plugin
            {
                assertEquals("Test Class Loaded Plugin 2", plugin.getName());
                assertEquals("test.atlassian.plugin.classloaded2", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                MockAnimalModuleDescriptor poohDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("pooh");
                assertEquals("Pooh Bear", poohDescriptor.getName());
                MockBear pooh = (MockBear)poohDescriptor.getModule();
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

    public void testSupportsAddition()
    {
        assertTrue(loader.supportsAddition());
    }

    public void testSupportsRemoval()
    {
        assertTrue(loader.supportsRemoval());
    }

    public void testMissingPlugins() throws PluginParseException
    {
        loader.loadAllPlugins(moduleDescriptorFactory);

        Collection col = loader.removeMissingPlugins();

        assertTrue(col.isEmpty());

        //delete safe copy of paddington plugin
        File paddington = new File(tempDir + File.separator + "plugins" + File.separator + PADDINGTON_JAR);
        paddington.delete();

        col = loader.removeMissingPlugins();

        assertEquals(1, col.size());
    }

    public void testNoFoundPlugins()
    {
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

        loader.loadAllPlugins(moduleDescriptorFactory);

        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);

        Collection col = loader.addFoundPlugins(moduleDescriptorFactory);

        assertEquals(1, col.size());
    }

    public void testRemovePlugin() throws PluginException, IOException
    {
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



}
