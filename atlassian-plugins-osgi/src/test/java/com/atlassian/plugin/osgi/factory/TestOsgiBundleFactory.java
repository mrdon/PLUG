package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;

public class TestOsgiBundleFactory extends TestCase {

    OsgiBundleFactory deployer;
    Mock mockOsgi;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        deployer = new OsgiBundleFactory((OsgiContainerManager) mockOsgi.proxy(), new DefaultPluginEventManager());
    }

    @Override
    public void tearDown()
    {
        deployer = null;
    }
    public void testCanDeploy() throws PluginParseException, IOException
    {
        File bundle = new PluginJarBuilder("someplugin")
            .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-Version: 1.0\n")
            .build();
        assertEquals("my.foo.symbolicName-1.0", deployer.canCreate(new JarPluginArtifact(new DeploymentUnit(bundle))));
    }

    public void testCanDeployStaticPluginWithManifest() throws PluginParseException, IOException
    {
        File bundle = new PluginJarBuilder("someplugin")
            .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-Version: 1.0\n")
            .addPluginInformation("foo", "Foo", "1.0", 1)
            .build();
        assertEquals(null, deployer.canCreate(new JarPluginArtifact(new DeploymentUnit(bundle))));
    }

    public void testCanDeployNoBundle() throws IOException, PluginParseException {

        File plugin = new PluginJarBuilder("someplugin")
            .addPluginInformation("my.foo.symb", "name", "1.0")
            .build();
        assertNull(deployer.canCreate(new JarPluginArtifact(new DeploymentUnit(plugin))));
    }

    public void testCanDeployNonJar() throws IOException, PluginParseException {

        final File tmp = File.createTempFile("foo", "bar");
        assertNull(deployer.canCreate(new PluginArtifact()
        {
            public DeploymentUnit getDeploymentUnit()
            {
                return new DeploymentUnit(tmp);
            }

            public boolean doesResourceExist(String name)
            {
                return false;
            }

            public InputStream getResourceAsStream(String fileName) throws PluginParseException {
                return null;
            }

            public String getName() {
                return tmp.getPath();
            }

            public InputStream getInputStream() {
                try {
                    return new FileInputStream(tmp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public File toFile()
            {
                return tmp;
            }
        }));
    }

    public void testDeploy() throws PluginParseException, IOException {
        File bundle = new PluginJarBuilder("someplugin")
            .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n")
            .build();

        Mock mockBundle = new Mock(Bundle.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        dict.put(Constants.BUNDLE_VENDOR, "acme");
        dict.put(Constants.BUNDLE_NAME, "myplugin");
        mockBundle.matchAndReturn("getHeaders", dict);
        mockBundle.expectAndReturn("getSymbolicName", "my.foo.symbolicName");
        mockOsgi.expectAndReturn("installBundle", C.ANY_ARGS, mockBundle.proxy());
        Plugin plugin = deployer.create(new JarPluginArtifact(new DeploymentUnit(bundle)), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiBundlePlugin);
        assertEquals("acme", plugin.getPluginInformation().getVendorName());
        assertEquals("myplugin", plugin.getName());
        assertEquals("desc", plugin.getPluginInformation().getDescription());
        assertNull(plugin.getI18nNameKey());
        mockOsgi.verify();
    }

    public void testDeployFail() throws PluginParseException, IOException {
        File bundle = new PluginJarBuilder("someplugin")
            .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n")
            .build();
        //noinspection ThrowableInstanceNeverThrown
        mockOsgi.expectAndThrow("installBundle", C.ANY_ARGS, new OsgiContainerException("Bad install"));
        Plugin plugin = deployer.create(new JarPluginArtifact(new DeploymentUnit(bundle)), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }
}
