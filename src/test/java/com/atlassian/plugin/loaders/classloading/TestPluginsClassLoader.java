package com.atlassian.plugin.loaders.classloading;

import junit.framework.TestCase;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import com.atlassian.plugin.loaders.TestClassPathPluginLoader;
import com.atlassian.plugin.mock.MockBear;
import com.atlassian.plugin.util.ClassLoaderUtils;

public class TestPluginsClassLoader extends TestCase
{
    public void testLoaderWithDirectory() throws URISyntaxException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        // hacky way of getting to the directoryPluginLoaderFiles classloading
        URL url = ClassLoaderUtils.getResource("test-disabled-plugin.xml", TestClassPathPluginLoader.class);
        File disabledPluginXml = new File(new URI(url.toExternalForm()));
        System.out.println("disabledPluginXml = " + disabledPluginXml);
        File directoryPluginLoaderFiles = new File(disabledPluginXml.getParentFile().getParentFile(), "classLoadingTestFiles");
        File pluginsDirectory = new File(directoryPluginLoaderFiles, "plugins");

        System.out.println("pluginsDirectory = " + pluginsDirectory);

        // first make a classloader of the entire directory.
        PluginsClassLoader loader = PluginsClassLoader.getInstance(pluginsDirectory.toURL());

        // check we got the right one back
        assertEquals(DirectoryClassLoader.class, loader.getClass());

        // we should get one descriptor back for each JAR in the directory (2)
        Enumeration descriptors = loader.findResources("paddington-test-plugin.jar");
        assertTrue(descriptors.hasMoreElements());
        URL descriptor = (URL) descriptors.nextElement();
        assertTrue(descriptor.toExternalForm().endsWith("plugins/src/test/directoryPluginLoaderFiles/plugins/paddington-test-plugin.jar"));
        assertFalse(descriptors.hasMoreElements());

        descriptors = loader.findResources("pooh-test-plugin.jar");
        assertTrue(descriptors.hasMoreElements());
        descriptor = (URL) descriptors.nextElement();
        assertTrue(descriptor.toExternalForm().endsWith("plugins/src/test/directoryPluginLoaderFiles/plugins/pooh-test-plugin.jar"));
        assertFalse(descriptors.hasMoreElements());


        // now try to load the plugin class and cast it to something in the global class path
        Class paddingtonClass = loader.findClass("com.atlassian.plugin.mock.MockPooh");
        MockBear paddington = (MockBear) paddingtonClass.newInstance();

        // check we really did get the right class
        assertEquals("com.atlassian.plugin.mock.MockPooh", paddington.getClass().getName());
    }
}
