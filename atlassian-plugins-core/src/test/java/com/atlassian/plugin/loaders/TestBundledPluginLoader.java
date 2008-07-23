package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.test.PluginBuilder;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestBundledPluginLoader extends TestCase
{
    private File pluginDir;

    public void setUp() throws IOException, URISyntaxException
    {
        pluginDir = new File(new File(System.getProperty("java.io.tmpdir")), "test-plugin-dir");
        pluginDir.mkdir();
    }

    public void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(pluginDir);
        pluginDir = null;
    }

    public void testCreateWithUnzip() throws IOException {
        File bundledZip = new PluginBuilder("bundledPlugins")
            .addResource("foo.txt", "foo")
            .build();

        new BundledPluginLoader(bundledZip.toURL(), pluginDir, null, null);
        assertEquals(2, pluginDir.list().length);
        assertTrue(new File(pluginDir, "foo.txt").exists());
    }
}