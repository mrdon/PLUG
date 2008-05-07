package com.atlassian.plugin.classloader;

import junit.framework.TestCase;

import java.net.URL;
import java.io.File;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 */
public class TestPluginClassLoader extends TestCase
{
    private static final String TEST_JAR = "testjars/atlassian-plugins-simpletest-1.0.jar";

    public void testPluginClassLoaderFindsInnerJars() throws Exception
    {
        URL url = getClass().getClassLoader().getResource(TEST_JAR);
        assertNotNull("Can't find test resource", url);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(new File(url.toURI()));
        List innerJars = pluginClassLoader.getInnerJars();
        assertEquals(2,innerJars.size());
        assertTrue(innerJars.contains("META-INF/lib/atlassian-plugins-innerjarone-1.0.jar"));
        assertTrue(innerJars.contains("META-INF/lib/atlassian-plugins-innerjartwo-1.0.jar"));
    }

    public void testPluginClassLoaderLoadsResourceFromOuterJarFirst() throws Exception
    {
        URL url = getClass().getClassLoader().getResource(TEST_JAR);
        assertNotNull("Can't find test resource", url);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(new File(url.toURI()));
        URL resourceUrl = pluginClassLoader.findResource("testresource.txt");
        assertNotNull(resourceUrl);
        assertEquals("outerjar",IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderLoadsClassFromOuterJar() throws Exception
    {
        URL url = getClass().getClassLoader().getResource(TEST_JAR);
        assertNotNull("Can't find test resource", url);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(new File(url.toURI()));
        Class c = pluginClassLoader.loadClass("com.atlassian.plugin.simpletest.TestClassOne");
        assertEquals("com.atlassian.plugin.simpletest",c.getPackage().getName());  // PLUG-27
        assertEquals("com.atlassian.plugin.simpletest.TestClassOne",c.getName());

//        URL resourceUrl = pluginClassLoader.findResource("testresource.txt");
//        assertNotNull(resourceUrl);
//        assertEquals("outerjar",IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderLoadsResourceFromInnerJarIfNotInOuterJar() throws Exception
    {
        URL url = getClass().getClassLoader().getResource(TEST_JAR);
        assertNotNull("Can't find test resource", url);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(new File(url.toURI()));
        URL resourceUrl = pluginClassLoader.findResource("innerresource.txt");
        assertNotNull(resourceUrl);
        assertEquals("innerresource",IOUtils.toString(resourceUrl.openStream()));
    }

}
