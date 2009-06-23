package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.test.PluginTestUtils;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.apache.commons.io.FileUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

public class TestOsgiPluginFactory extends TestCase
{
    OsgiPluginFactory factory;

    private File tmpDir;
    private File jar;
    Mock mockOsgi;
    private Mock mockBundle;
    private Mock mockSystemBundle;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        tmpDir = PluginTestUtils.createTempDirectory(TestOsgiPluginFactory.class);
        mockOsgi = new Mock(OsgiContainerManager.class);
        factory = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, new DefaultOsgiPersistentCache(tmpDir), (OsgiContainerManager) mockOsgi.proxy(), new DefaultPluginEventManager());
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "1.0").build();

        mockBundle = new Mock(Bundle.class);
        final Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);

        mockSystemBundle = new Mock(Bundle.class);
        final Dictionary<String, String> sysDict = new Hashtable<String, String>();
        sysDict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        sysDict.put(Constants.BUNDLE_VERSION, "1.0");
        mockSystemBundle.matchAndReturn("getHeaders", sysDict);
        mockSystemBundle.matchAndReturn("getLastModified", System.currentTimeMillis());
        mockSystemBundle.matchAndReturn("getSymbolicName", "system.bundle");

        Mock mockSysContext = new Mock(BundleContext.class);
        mockSystemBundle.matchAndReturn("getBundleContext", mockSysContext.proxy());

        mockSysContext.matchAndReturn("getServiceReference", C.ANY_ARGS, null);
        mockSysContext.matchAndReturn("getService", C.ANY_ARGS, new Mock(PackageAdmin.class).proxy());
    }

    @Override
    public void tearDown() throws IOException
    {
        factory = null;
        FileUtils.cleanDirectory(tmpDir);
        jar.delete();
    }

    public void testCreateOsgiPlugin() throws PluginParseException
    {
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        mockOsgi.expectAndReturn("getHostComponentRegistrations", new ArrayList());
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        mockOsgi.matchAndReturn("getBundles", new Bundle[] {(Bundle) mockSystemBundle.proxy()});
        final Plugin plugin = factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiPlugin);
        mockOsgi.verify();
    }

    public void testCreateOsgiPluginWithBadVersion() throws PluginParseException, IOException
    {
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "beta.1.0").build();
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        mockOsgi.matchAndReturn("getBundles", new Bundle[] {(Bundle) mockSystemBundle.proxy()});
        try
        {
            factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
            fail("Should have complained about osgi version");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        mockOsgi.verify();
    }

    public void testCreateOsgiPluginWithEmptyVersion() throws PluginParseException, IOException
    {
        jar = new PluginJarBuilder("someplugin").addPluginInformation("plugin.key", "My Plugin", "").build();
        mockBundle.expectAndReturn("getSymbolicName", "plugin.key");
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        mockOsgi.matchAndReturn("getBundles", new Bundle[] {(Bundle) mockSystemBundle.proxy()});
        try
        {
            factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
            fail("Should have complained about osgi version");
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
        mockOsgi.verify();
    }

    /**
    public void testCreateOsgiPluginFail() throws PluginParseException
    {
        //noinspection ThrowableInstanceNeverThrown
        mockOsgi.expectAndReturn("getServiceTracker", C.ANY_ARGS, null);
        mockOsgi.expectAndReturn("getHostComponentRegistrations", new ArrayList());
        mockOsgi.matchAndReturn("getBundles", new Bundle[] {(Bundle) mockSystemBundle.proxy()});
        final Plugin plugin = factory.create(new JarPluginArtifact(jar), (ModuleDescriptorFactory) new Mock(ModuleDescriptorFactory.class).proxy());
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }
     **/

    public void testCanLoadWithXml() throws PluginParseException, IOException
    {
        final File plugin = new PluginJarBuilder("loadwithxml").addPluginInformation("foo.bar", "", "1.0").build();
        final String key = factory.canCreate(new JarPluginArtifact(plugin));
        assertEquals("foo.bar", key);
    }

    public void testCanLoadNoXml() throws PluginParseException, IOException
    {
        final File plugin = new PluginJarBuilder("loadwithxml").build();
        final String key = factory.canCreate(new JarPluginArtifact(plugin));
        assertNull(key);
    }

}
