package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
import com.atlassian.plugin.artifact.PluginArtifactFactory;
import com.atlassian.plugin.artifact.DefaultPluginArtifactFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
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
    final PluginArtifactFactory factory = new DefaultPluginArtifactFactory();

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
        assertNull(deployer.canCreate(factory.create(plugin)));
    }

    public void testCanDeployNonJar() throws IOException, PluginParseException {

        final File tmp = File.createTempFile("foo", ".xml");
        assertNull(deployer.canCreate(factory.create(tmp)));
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
        Plugin plugin = deployer.create(factory.create(bundle), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
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
        Plugin plugin = deployer.create(factory.create(bundle), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }
}
