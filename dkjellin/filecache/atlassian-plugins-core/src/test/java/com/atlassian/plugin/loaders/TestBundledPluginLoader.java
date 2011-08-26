package com.atlassian.plugin.loaders;

import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.test.PluginTestUtils;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TestBundledPluginLoader extends TestCase
{
    private File pluginDir;

    public void setUp() throws IOException, URISyntaxException
    {
        pluginDir = new File("target/test-plugin-dir");
        pluginDir.mkdir();
    }

    public void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(pluginDir);
        pluginDir = null;
    }

    public void testCreateWithUnzip() throws IOException {
        File bundledZip = new PluginJarBuilder("bundledPlugins")
            .addResource("foo.txt", "foo")
            .build();

        final BundledPluginLoader loader = new BundledPluginLoader(bundledZip.toURL(), pluginDir, Collections.<PluginFactory>emptyList(), new DefaultPluginEventManager());
        assertLoaderContains(loader, "foo.txt");
    }

    public void testCreateWithDirectoryOfFiles() throws IOException {
        final File dir = PluginTestUtils.createTempDirectory(TestBundledPluginLoader.class);
        FileUtils.writeStringToFile(new File(dir, "foo.txt"), "hello");

        final BundledPluginLoader loader = new BundledPluginLoader(dir.toURL(), pluginDir, Collections.<PluginFactory>emptyList(), new DefaultPluginEventManager());
        assertLoaderContains(loader, "foo.txt");
    }

    public void testCreateWithListFile() throws IOException {
        final File dir = PluginTestUtils.createTempDirectory(TestBundledPluginLoader.class);
        FileUtils.writeStringToFile(new File(dir, "foo.txt"), "hello");
        FileUtils.writeStringToFile(new File(dir, "bar.txt"), "world");
        File listFile = new File(dir, "bundled-plugins.list");
        FileUtils.writeStringToFile(listFile, "foo.txt\nbar.txt");

        final BundledPluginLoader loader = new BundledPluginLoader(listFile.toURL(), pluginDir, Collections.<PluginFactory>emptyList(), new DefaultPluginEventManager());
        assertLoaderContains(loader, "foo.txt", "bar.txt");
    }

    private void assertLoaderContains(final BundledPluginLoader loader, String ... expectedEntries)
    {
        List<String> expected = new ArrayList<String>(Arrays.asList(expectedEntries));

        final Collection<DeploymentUnit> scanned = loader.scanner.scan();

        for (DeploymentUnit unit : scanned)
        {
            final File file = unit.getPath();
            if (file.isDirectory())
            {
                    assertEquals("META-INF", file.getName());
            }
            else
            {
                assertTrue("found file in scanner " + file + " but expected " + expected, expected.contains(file.getName()));
                expected.remove(file.getName());
            }
        }
        assertTrue("Contained: " + expected, expected.isEmpty());
    }

}