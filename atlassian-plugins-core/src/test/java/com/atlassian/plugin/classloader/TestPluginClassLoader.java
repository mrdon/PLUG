package com.atlassian.plugin.classloader;

import com.atlassian.plugin.test.PluginTestUtils;
import com.opensymphony.util.BeanUtils;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * To make this test pass in your IDE, make sure you run Maven to build the necessary plugins and copy
 * them to the target directory ('mvn package') and when running the tests, set the 'project.version'
 * system property to the current version in the POM. E.g. -Dproject.version=2.1.0-SNAPSHOT
 */
public class TestPluginClassLoader extends TestCase
{
    private PluginClassLoader pluginClassLoader;
    private File tmpDir;

    protected void setUp() throws Exception
    {
        super.setUp();
        tmpDir = new File("target/" + this.getClass().getName());
        tmpDir.mkdirs();

        URL url = getClass().getClassLoader().getResource(PluginTestUtils.SIMPLE_TEST_JAR);
        assertNotNull("Can't find test resource -- see class Javadoc, and " +
            "be sure to set the 'project.version' system property!", url);
        pluginClassLoader = new PluginClassLoader(new File(url.getFile()), getClass().getClassLoader(), tmpDir);
    }

    protected void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(tmpDir);
        super.tearDown();
    }

    public void testPluginClassLoaderFindsInnerJars() throws Exception
    {
        List innerJars = pluginClassLoader.getPluginInnerJars();
        assertEquals(2, innerJars.size());
    }

    public void testPluginClassLoaderLoadsResourceFromOuterJarFirst() throws Exception
    {
        URL resourceUrl = pluginClassLoader.getResource("testresource.txt");
        assertNotNull(resourceUrl);
        assertEquals("outerjar", IOUtils.toString(resourceUrl.openStream()));
    }

    public void testPluginClassLoaderLoadsClassFromOuterJar() throws Exception
    {
        Class c = pluginClassLoader.loadClass("com.atlassian.plugin.simpletest.TestClassOne");
        assertEquals("com.atlassian.plugin.simpletest", c.getPackage().getName());  // PLUG-27
        assertEquals("com.atlassian.plugin.simpletest.TestClassOne", c.getName());
    }

    public void testPluginClassLoaderHandlesDeletedExctractedInnerJars() throws Exception
    {
        InputStream inputStream = pluginClassLoader.getResource("innerresource.txt").openStream();
        assertNotNull(inputStream);
        inputStream.close();
        FileUtils.deleteDirectory(tmpDir);
        assertTrue(tmpDir.mkdirs());
        try
        {
            assertNotNull(pluginClassLoader.getResource("innerresource.txt").openStream());
            fail("underlying extracted inner jar was deleted and should throw FileNotFoundException");
        }
        catch (IOException e)
        {
            // expected exception because we deleted the jar
        }
    }

    public void testPluginClassLoaderExtractsInnerJarsToSpecifiedDirectory() throws Exception
    {
        final List<File> files = pluginClassLoader.getPluginInnerJars();
        for (File file : files)
        {
            assertEquals(tmpDir, file.getParentFile());
        }
    }

    public void testPluginClassLoaderDetectsMissingTempDirectory() throws Exception
    {
        try
        {
            new PluginClassLoader(null, getClass().getClassLoader(), new File("/doesnotexisthopefully"));
            fail("should throw IllegalArgumentException when temp directory does not exist");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testPluginClassLoaderLoadsResourceFromInnerJarIfNotInOuterJar() throws Exception
    {
        final URL resourceUrl = pluginClassLoader.getResource("innerresource.txt");
        assertNotNull(resourceUrl);
        InputStream inputStream = resourceUrl.openStream();
        assertEquals("innerresource", IOUtils.toString(inputStream));
        inputStream.close();
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
            2, BeanUtils.getValue(instance, "version"));
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
