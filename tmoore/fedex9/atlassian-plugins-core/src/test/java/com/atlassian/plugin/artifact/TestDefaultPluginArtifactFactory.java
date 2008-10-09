package com.atlassian.plugin.artifact;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.test.PluginBuilder;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
        PluginArtifact jarArt = factory.create(jarFile);
        assertNotNull(jarArt);
        assertTrue(jarArt instanceof JarPluginArtifact);

        PluginArtifact xmlArt = factory.create(xmlFile);
        assertNotNull(xmlArt);
        assertTrue(xmlArt instanceof AtomicPluginArtifact);

        try
        {

            factory.create(new File(testDir, "bob.jim"));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // test passed
        }
    }
}
