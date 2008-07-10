package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import junit.framework.TestCase;
import org.twdata.pkgscanner.ExportPackage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

        felix = new FelixOsgiContainerManager(frameworkBundlesUrl, frameworkBundlesDir, new DefaultPackageScannerConfiguration());
    }

    @Override
    public void tearDown() throws Exception
    {
        felix = null;
        tmpdir = null;
        FileUtils.deleteDirectory(frameworkBundlesDir);
    }

    public void testDeleteDirectory() throws IOException
    {
        File tmp = File.createTempFile("foo", "bar").getParentFile();
        File dir = new File(tmp, "base");
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
        File tmpdir = File.createTempFile("foo", "bar").getParentFile();
        File dir = new File(tmpdir, "felix");
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        felix.initialiseCacheDirectory();
        assertTrue(dir.exists());
        assertTrue(dir.listFiles().length == 0);
    }

    public void testConstructAutoExports()
    {
        List<ExportPackage> exports = new ArrayList<ExportPackage>();
        exports.add(new ExportPackage("foo.bar", "1.0"));
        exports.add(new ExportPackage("foo.bar", "1.0-asdf-asdf"));
        assertEquals("foo.bar;version=1.0,foo.bar", felix.constructAutoExports(exports));
    }

    public void testStartStop()
    {
        felix.start(null);
        assertTrue(felix.isRunning());
        assertEquals(1, felix.getBundles().length);
        felix.stop();
        assertTrue(!felix.isRunning());
    }

    public void testInstallBundle() throws URISyntaxException
    {
        felix.start(null);
        assertEquals(1, felix.getBundles().length);
        File jar = new File(getClass().getResource("/myapp-1.0.jar").toURI());
        felix.installBundle(jar);
        assertEquals(2, felix.getBundles().length);
        felix.stop();
        assertTrue(!felix.isRunning());
    }

    public void testReloadHostComponents()
    {
        HostComponentProvider prov = new HostComponentProvider() {

            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(Serializable.class).forInstance("Some String");
            }
        };
        felix.start(null);
        assertTrue(felix.isRunning());
        assertEquals(1, felix.getBundles().length);
        assertEquals(2, felix.getRegisteredServices().length);
        felix.reloadHostComponents(prov);
        assertEquals(3, felix.getRegisteredServices().length);
        felix.reloadHostComponents(prov);
        assertEquals(3, felix.getRegisteredServices().length);
        felix.stop();
        assertTrue(!felix.isRunning());
    }
}
