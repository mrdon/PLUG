package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ArrayList;

import com.atlassian.plugin.*;
import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

public class TestOsgiPluginDeployer extends TestCase
{
    OsgiPluginFactory deployer;

    private File jar;
    Mock mockOsgi;
    private Mock mockBundle;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        deployer = new OsgiPluginFactory(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, (OsgiContainerManager) mockOsgi.proxy());
        this.jar = new PluginBuilder("someplugin")
            .addPluginInformation("plugin.key", "My Plugin", "1.0")
            .build();

        mockBundle = new Mock(Bundle.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);
    }

    @Override
    public void tearDown()
    {
        deployer = null;
        this.jar.delete();
    }

    public void testCreateOsgiPlugin() throws PluginParseException {
        mockOsgi.expectAndReturn("installBundle", C.ANY_ARGS, mockBundle.proxy());
        mockOsgi.expectAndReturn("getHostComponentRegistrations", new ArrayList());
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        Plugin plugin = deployer.create(new DeploymentUnit(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiPlugin);
        mockOsgi.verify();
    }

    public void testCreateOsgiPluginFail() throws PluginParseException {
        //noinspection ThrowableInstanceNeverThrown
        mockOsgi.expectAndThrow("installBundle", C.ANY_ARGS, new OsgiContainerException("Bad install"));
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        mockOsgi.expectAndReturn("getHostComponentRegistrations", new ArrayList());
        Plugin plugin = deployer.create(new DeploymentUnit(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }

    public void testCanLoadWithXml() throws PluginParseException, IOException {
        File plugin = new PluginBuilder("loadwithxml").addPluginInformation("foo.bar", "", "1.0").build();
        String key = deployer.canCreate(new JarPluginArtifact(plugin));
        assertEquals("foo.bar", key);
    }

    public void testCanLoadNoXml() throws PluginParseException, IOException {
        File plugin = new PluginBuilder("loadwithxml").build();
        String key = deployer.canCreate(new JarPluginArtifact(plugin));
        assertNull(key);
    }
}
