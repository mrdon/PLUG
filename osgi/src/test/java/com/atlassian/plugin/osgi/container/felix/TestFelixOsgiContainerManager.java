package com.atlassian.plugin.osgi.container.felix;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.net.URISyntaxException;

import org.twdata.pkgscanner.ExportPackage;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.loader.OsgiPluginLoader;

public class TestFelixOsgiContainerManager extends TestCase
{
    File tmpdir;
    File startBundlesDir;
    private FelixOsgiContainerManager felix;

    @Override
    public void setUp() throws IOException
    {
        tmpdir = File.createTempFile("foo", "bar").getParentFile();
        startBundlesDir = new File(tmpdir, "startBundles");
        startBundlesDir.mkdir();

        felix = new FelixOsgiContainerManager(OsgiPluginLoader.FRAMEWORK_BUNDLES_BASE_PATH);
    }

    @Override
    public void tearDown()
    {
        FelixOsgiContainerManager.deleteDirectory(startBundlesDir);
        felix = null;
        tmpdir = null;
    }

    public void testDeleteDirectory() throws IOException
    {
        File tmp = File.createTempFile("foo", "bar").getParentFile();
        File dir = new File(tmp, "base");
        dir.mkdir();
        File subdir = new File(dir, "subdir");
        subdir.mkdir();
        File kid = File.createTempFile("foo", "bar", subdir);

        FelixOsgiContainerManager.deleteDirectory(dir);
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
        felix.start(Collections.EMPTY_LIST, null);
        assertTrue(felix.isRunning());
        assertEquals(1, felix.getBundles().length);
        felix.stop();
        assertTrue(!felix.isRunning());
    }

    public void testInstallBundle() throws URISyntaxException
    {
        felix.start(Collections.EMPTY_LIST, null);
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
        felix.start(Collections.EMPTY_LIST, null);
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
