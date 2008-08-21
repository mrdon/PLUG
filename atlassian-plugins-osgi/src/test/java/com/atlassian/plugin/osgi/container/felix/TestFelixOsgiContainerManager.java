package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.ConstraintMatcher;
import com.mockobjects.dynamic.C;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class TestFelixOsgiContainerManager extends TestCase
{
    private File tmpdir;
    private FelixOsgiContainerManager felix;
    private URL frameworkBundlesUrl = getClass().getResource("/nothing.zip");
    private File frameworkBundlesDir;

    @Override
    public void setUp() throws IOException
    {
        tmpdir = new File(System.getProperty("java.io.tmpdir"));
        frameworkBundlesDir = new File(tmpdir, "framework-bundles-test");

        felix = new FelixOsgiContainerManager(frameworkBundlesUrl, frameworkBundlesDir, new DefaultPackageScannerConfiguration(),
                null, new DefaultPluginEventManager());
    }

    @Override
    public void tearDown() throws Exception
    {
        if (felix != null)
            felix.stop();
        felix = null;
        tmpdir = null;
        FileUtils.deleteDirectory(frameworkBundlesDir);
    }

    public void testDeleteDirectory() throws IOException
    {
        File dir = new File(tmpdir, "base");
        dir.mkdir();
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        File kid = File.createTempFile("foo", "bar", subdir);

        FileUtils.deleteDirectory(dir);
        assertTrue(!kid.exists());
        assertTrue(!subdir.exists());
        assertTrue(!dir.exists());
    }

    public void testInitialiseCacheDirectory() throws IOException
    {
        File dir = new File(tmpdir, "felix");
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        felix.initialiseCacheDirectory();
        assertTrue(dir.exists());
        assertTrue(dir.listFiles().length == 0);
    }

    public void testStartStop()
    {
        felix.start();
        assertTrue(felix.isRunning());
        assertEquals(1, felix.getBundles().length);
    }

    public void testInstallBundle() throws URISyntaxException
    {
        felix.start();
        assertEquals(1, felix.getBundles().length);
        File jar = new File(getClass().getResource("/myapp-1.0.jar").toURI());
        felix.installBundle(jar);
        assertEquals(2, felix.getBundles().length);
    }

    public void testInstallBundleTwice() throws URISyntaxException, IOException, BundleException
    {
        File plugin = new PluginBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("foo.txt", "foo")
                .build();

        File pluginUpdate = new PluginBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n")
                .addResource("bar.txt", "bar")
                .build();

        felix.start();
        assertEquals(1, felix.getBundles().length);
        Bundle bundle = felix.installBundle(plugin);
        assertEquals(2, felix.getBundles().length);
        assertEquals("my.foo.symbolicName", bundle.getSymbolicName());
        assertEquals("1.0", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        assertEquals(Bundle.INSTALLED, bundle.getState());
        assertNotNull(bundle.getResource("foo.txt"));
        assertNull(bundle.getResource("bar.txt"));
        bundle.start();
        assertEquals(Bundle.ACTIVE, bundle.getState());
        Bundle bundleUpdate = felix.installBundle(pluginUpdate);
        assertEquals(2, felix.getBundles().length);
        assertEquals(Bundle.INSTALLED, bundleUpdate.getState());
        bundle.start();
        assertNull(bundleUpdate.getResource("foo.txt"));
        assertNotNull(bundleUpdate.getResource("bar.txt"));
    }

    public void testInstallFailure() throws Exception
    {
        File plugin = new PluginBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Import-Package: foo.missing.package\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ManifestVersion: 2\n" )
                .build();
        felix.start();

        Bundle bundle = felix.installBundle(plugin);
        try {
            bundle.loadClass("foo.bar");
            fail("Should have thrown exception");
        } catch (ClassNotFoundException ex) {
            // no worries
        }
    }
}
