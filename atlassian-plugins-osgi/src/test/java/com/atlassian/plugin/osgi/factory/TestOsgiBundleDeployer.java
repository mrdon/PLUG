package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
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

public class TestOsgiBundleDeployer extends TestCase {

    OsgiBundleFactory deployer;
    Mock mockOsgi;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        deployer = new OsgiBundleFactory((OsgiContainerManager) mockOsgi.proxy());
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
                        "Bundle-SymbolicName: my.foo.symbolicName\n")
            .build();
        assertEquals("my.foo.symbolicName", deployer.canCreate(new JarPluginArtifact(bundle)));
    }

    public void testCanDeployNoBundle() throws IOException, PluginParseException {

        File plugin = new PluginJarBuilder("someplugin")
            .addPluginInformation("my.foo.symb", "name", "1.0")
            .build();
        assertNull(deployer.canCreate(new JarPluginArtifact(plugin)));
    }

    public void testCanDeployNonJar() throws IOException, PluginParseException {

        final File tmp = File.createTempFile("foo", "bar");
        assertNull(deployer.canCreate(new PluginArtifact()
        {
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
        mockBundle.matchAndReturn("getHeaders", dict);
        mockOsgi.expectAndReturn("installBundle", C.ANY_ARGS, mockBundle.proxy());
        Plugin plugin = deployer.create(new DeploymentUnit(bundle), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiPlugin);
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
        Plugin plugin = deployer.create(new DeploymentUnit(bundle), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }
}