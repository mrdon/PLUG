package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestBundledOsgiPluginLoader extends TestCase
{
    File tmpdir;
    File startBundlesDir;
    Mock mockOsgi;
    private File pluginDir;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        tmpdir = new File(System.getProperty("java.io.tmpdir"));
        pluginDir = new File(tmpdir, "test-plugin-dir");
        pluginDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(pluginDir);
        tmpdir = null;
    }

    public void testCreateWithUnzip()
    {
        new BundledOsgiPluginLoader(getClass().getResource("/bundled-plugins-test.zip"), pluginDir,
                PluginManager.PLUGIN_DESCRIPTOR_FILENAME, null, (OsgiContainerManager) mockOsgi.proxy(), null);
        String[] files = pluginDir.list();
        assertEquals(1, files.length);
        assertEquals("myapp-1.0-plugin2.jar", files[0]);
    }
}
