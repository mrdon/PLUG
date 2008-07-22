package com.atlassian.plugin.classloader;

import com.atlassian.plugin.test.PluginTestUtils;
import com.opensymphony.util.BeanUtils;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 */
public class TestPluginClassLoader extends TestCase
{
    private PluginClassLoader pluginClassLoader;

    protected void setUp() throws Exception
    {
        URL url = getClass().getClassLoader().getResource(PluginTestUtils.SIMPLE_TEST_JAR);
        assertNotNull("Can't find test resource", url);
        pluginClassLoader = new PluginClassLoader(new File(url.getFile()), getClass().getClassLoader());
    }

    public void testPluginClassLoaderFindsInnerJars() throws Exception
    {
        List innerJars = pluginClassLoader.getPluginInnerJars();
        assertEquals(2,innerJars.size());
    }

    public void testPluginClassLoaderLoadsResourceFromOuterJarFirst() throws Exception
    {
        URL resourceUrl = pluginClassLoader.getResource("testresource.txt");
        assertNotNull(resourceUrl);
        assertEquals("outerjar",IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderLoadsClassFromOuterJar() throws Exception
    {
        Class c = pluginClassLoader.loadClass("com.atlassian.plugin.simpletest.TestClassOne");
        assertEquals("com.atlassian.plugin.simpletest",c.getPackage().getName());  // PLUG-27
        assertEquals("com.atlassian.plugin.simpletest.TestClassOne",c.getName());

//        URL resourceUrl = pluginClassLoader.findResource("testresource.txt");
//        assertNotNull(resourceUrl);
//        assertEquals("outerjar",IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderLoadsResourceFromInnerJarIfNotInOuterJar() throws Exception
    {
        final URL resourceUrl = pluginClassLoader.getResource("innerresource.txt");
        assertNotNull(resourceUrl);
        assertEquals("innerresource",IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderDoesNotSwallowClassesFromADifferentClassLoader() throws Exception
    {
        final Class c = Class.forName(getClass().getName(), true, pluginClassLoader);
        assertEquals(getClass().getClassLoader(), c.getClassLoader());
    }

    public void testPluginClassLoaderOverridesContainerClassesWithInnerJarClasses() throws Exception
    {
        Class mockVersionedClass =
            Class.forName("com.atlassian.plugin.mock.MockVersionedClass", true, pluginClassLoader);
        Object instance = mockVersionedClass.newInstance();

        assertEquals("PluginClassLoader is searching the parent classloader for classes before inner JARs",
                     new Integer(2), BeanUtils.getValue(instance, "version"));
    }

    public void testPluginClassLoaderDoesNotLockTheJarsPermanently() throws Exception
    {
        //N.B This will probably never fail on a non Windows machine
        String fileLoc = getClass().getClassLoader().getResource(PluginTestUtils.SIMPLE_TEST_JAR).getFile();
        File original = new File(fileLoc);
        File tmpFile = new File(fileLoc + ".tmp");
        FileUtils.copyFile(original, tmpFile);

        PluginClassLoader pluginClassLoaderThatHasNotLockedFileYet = new
                PluginClassLoader(tmpFile, getClass().getClassLoader());
        Class mockVersionedClass =
                Class.forName("com.atlassian.plugin.mock.MockVersionedClass", true, pluginClassLoader);
        assertTrue(tmpFile.delete());
    }
}
