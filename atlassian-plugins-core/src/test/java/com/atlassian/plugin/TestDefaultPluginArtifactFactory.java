package com.atlassian.plugin;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import com.atlassian.plugin.test.PluginBuilder;

public class TestDefaultPluginArtifactFactory extends TestCase
{
    File testDir;
    public void setUp() throws IOException
    {
        testDir = File.createTempFile("test", "");
        testDir.delete();
        testDir.mkdir();
    }

    public void tearDown() throws IOException
    {
        FileUtils.deleteDirectory(testDir);
    }
    public void testCreate() throws IOException
    {
        File xmlFile = new File(testDir, "foo.xml");
        FileUtils.writeStringToFile(xmlFile, "<xml/>");
        File jarFile = new PluginBuilder("jar").build(testDir);

        DefaultPluginArtifactFactory factory = new DefaultPluginArtifactFactory();
        PluginArtifact jarArt = factory.create(jarFile.toURL());
        assertNotNull(jarArt);
        assertTrue(jarArt instanceof JarPluginArtifact);

        PluginArtifact xmlArt = factory.create(xmlFile.toURL());
        assertNotNull(xmlArt);
        assertTrue(xmlArt instanceof XmlPluginArtifact);

        try
        {

            factory.create(new File(testDir, "bob.jim").toURL());
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // test passed
        }
    }
}
