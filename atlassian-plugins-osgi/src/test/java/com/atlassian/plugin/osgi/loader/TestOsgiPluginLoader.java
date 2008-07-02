package com.atlassian.plugin.osgi.loader;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collection;
import java.util.ArrayList;

import com.atlassian.plugin.*;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.loader.transform.PluginTransformer;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.twdata.pkgscanner.ExportPackage;

public class TestOsgiPluginLoader extends TestCase
{
    OsgiPluginLoader loader;

    File tmpdir;
    File startBundlesDir;
    private File jar;
    Mock mockOsgi;
    private Mock mockBundle;

    @Override
    public void setUp() throws IOException, URISyntaxException
    {
        mockOsgi = new Mock(OsgiContainerManager.class);
        tmpdir = File.createTempFile("foo", "bar").getParentFile();
        loader = new OsgiPluginLoader(tmpdir, PluginManager.PLUGIN_DESCRIPTOR_FILENAME, null, (OsgiContainerManager) mockOsgi.proxy(), null);
        this.jar = new File(getClass().getResource("/myapp-1.0.jar").toURI());

        mockBundle = new Mock(Bundle.class);
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        mockBundle.matchAndReturn("getHeaders", dict);
    }

    @Override
    public void tearDown()
    {
        loader = null;
        tmpdir = null;
    }

    public void testCreateOsgiPlugin()
    {
        mockOsgi.expectAndReturn("installBundle", C.args(C.eq(jar)), mockBundle.proxy());
        Plugin plugin = loader.createOsgiPlugin(jar, true);
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiBundlePlugin);
        mockOsgi.verify();
    }

    public void testCreateOsgiPluginFail()
    {
        //noinspection ThrowableInstanceNeverThrown
        mockOsgi.expectAndThrow("installBundle", C.args(C.eq(jar)), new OsgiContainerException("Bad install"));
        Plugin plugin = loader.createOsgiPlugin(jar, true);
        assertNotNull(plugin);
        assertTrue(plugin instanceof UnloadablePlugin);
        mockOsgi.verify();
    }

    public void testHandleNoDescriptor() throws PluginParseException
    {
        mockOsgi.expectAndReturn("installBundle", C.args(C.eq(jar)), mockBundle.proxy());
        DeploymentUnit unit = new DeploymentUnit(jar);
        Plugin plugin = loader.handleNoDescriptor(unit);
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiBundlePlugin);
        mockOsgi.verify();
    }

    public void testCanLoadWithXml() throws PluginParseException
    {
        Mock mockPluginJar = new Mock(PluginJar.class);
        PluginJar pluginJar = (PluginJar) mockPluginJar.proxy();

        mockPluginJar.expectAndReturn("getFile", C.args(C.eq(DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME)),
                new ByteArrayInputStream("<atlassian-plugin key=\"foo.bar\" />".getBytes()));
        String key = loader.canLoad(pluginJar);
        assertEquals("foo.bar", key);
        mockPluginJar.verify();
    }

    public void testCanLoadWithManifest() throws PluginParseException
    {
        Mock mockPluginJar = new Mock(PluginJar.class);
        PluginJar pluginJar = (PluginJar) mockPluginJar.proxy();

        mockPluginJar.expectAndReturn("getFile", C.args(C.eq(DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME)), null);
        mockPluginJar.expectAndReturn("getFile", C.args(C.eq("META-INF/MANIFEST.MF")),
                new ByteArrayInputStream("Manifest-Version: 1.0\nBundle-SymbolicName: foo.bar\n".getBytes()));
        String key = loader.canLoad(pluginJar);
        assertEquals("foo.bar", key);
        mockPluginJar.verify();
    }

    public void testHandleNoDescriptor_NoOsgiOrDescriptor() throws PluginParseException, URISyntaxException
    {
        this.jar = new File(getClass().getResource("/myapp-1.0-nonosgi.jar").toURI());
        DeploymentUnit unit = new DeploymentUnit(jar);
        try
        {
            loader.handleNoDescriptor(unit);
        } catch (PluginParseException ex)
        {
            // it worked
            return;
        }
        fail("Should have thrown an exception");
    }

    public void testCreatePlugin()
    {
        Mock mockPluginTrans = new Mock(PluginTransformer.class);
        loader.setPluginTransformer((PluginTransformer) mockPluginTrans.proxy());
        mockPluginTrans.expectAndReturn("transform", C.ANY_ARGS, jar);
        mockOsgi.expectAndReturn("getHostComponentRegistrations", new ArrayList());
        mockOsgi.expectAndReturn("installBundle", C.args(C.eq(jar)), mockBundle.proxy());
        Mock mockParser = new Mock(DescriptorParser.class);
        mockParser.expectAndReturn("getPluginsVersion", 2);
        mockBundle.expect("start");
        DeploymentUnit unit = new DeploymentUnit(jar);

        Plugin plugin = loader.createPlugin((DescriptorParser) mockParser.proxy(), unit, null);
        assertNotNull(plugin);
        assertTrue(plugin instanceof OsgiPlugin);
        mockParser.verify();
        mockPluginTrans.verify();
        mockBundle.verify();
    }

}
