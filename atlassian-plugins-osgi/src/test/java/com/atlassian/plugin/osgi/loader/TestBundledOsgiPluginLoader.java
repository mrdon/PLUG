package com.atlassian.plugin.osgi.loader;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;

import com.mockobjects.dynamic.Mock;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.util.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class TestBundledOsgiPluginLoader extends TestCase
{
    File tmpdir;
    File startBundlesDir;
    Mock mockOsgi;
    private Mock mockBundle;
    private File pluginDir;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        tmpdir = new File(System.getProperty("java.io.tmpdir"));
        pluginDir = new File(tmpdir, "test-plugin-dir");
        pluginDir.mkdir();

        mockBundle = new Mock(Bundle.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);
    }

    @Override
    public void tearDown()
    {
        FileUtils.deleteDir(pluginDir);
        tmpdir = null;
    }

    public void testCreateWithUnzip()
    {
        new BundledOsgiPluginLoader("bundled-plugins-test.zip", pluginDir,
                PluginManager.PLUGIN_DESCRIPTOR_FILENAME, null, (OsgiContainerManager) mockOsgi.proxy(), null);
        String[] files = pluginDir.list();
        assertEquals(1, files.length);
        assertEquals("myapp-1.0-plugin2.jar", files[0]);
    }
}
