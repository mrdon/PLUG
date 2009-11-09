package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.test.PluginTestUtils;

import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

public class TestDefaultPluginTransformer extends TestCase
{
    private DefaultPluginTransformer transformer;
    private File tmpDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tmpDir = PluginTestUtils.createTempDirectory("plugin-transformer");
        transformer = new DefaultPluginTransformer(new DefaultOsgiPersistentCache(tmpDir), SystemExports.NONE, null, PluginAccessor.Descriptor.FILENAME);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        tmpDir = null;
        transformer = null;
    }

    public void testAddFilesToZip() throws URISyntaxException, IOException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        final Map<String, byte[]> files = new HashMap<String, byte[]>()
        {
            {
                put("foo", "bar".getBytes());
            }
        };
        final File copy = transformer.addFilesToExistingZip(file, files);
        assertNotNull(copy);
        assertTrue(!copy.getName().equals(file.getName()));
        assertTrue(copy.length() != file.length());

        final ZipFile zip = new ZipFile(copy);
        final ZipEntry entry = zip.getEntry("foo");
        assertNotNull(entry);
    }

    public void testTransform() throws Exception
    {
        final File file = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  com.atlassian.plugin.osgi.factory.transform.Fooable bar;",
                        "}")
                .addPluginInformation("foo", "foo", "1.1")
                .build();

        final File copy = transformer.transform(new JarPluginArtifact(file), new ArrayList<HostComponentRegistration>()
        {
            {
                add(new StubHostComponentRegistration(Fooable.class));
            }
        });

        assertNotNull(copy);
        assertTrue(copy.getName().contains(String.valueOf(file.lastModified())));
        assertTrue(copy.getName().endsWith(".jar"));
        assertEquals(tmpDir.getAbsolutePath(), copy.getParentFile().getParentFile().getAbsolutePath());
        final JarFile jar = new JarFile(copy);
        final Attributes attrs = jar.getManifest().getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));

        assertNotNull(jar.getEntry("META-INF/spring/atlassian-plugins-host-components.xml"));
    }

    public void testGenerateCacheName() throws IOException
    {
        File tmp = File.createTempFile("asdf", ".jar", tmpDir);
        assertTrue(DefaultPluginTransformer.generateCacheName(tmp).endsWith(".jar"));
        tmp = File.createTempFile("asdf", "asdf", tmpDir);
        assertTrue(DefaultPluginTransformer.generateCacheName(tmp).endsWith(String.valueOf(tmp.lastModified())));

        tmp = File.createTempFile("asdf", "asdf.", tmpDir);
        assertTrue(DefaultPluginTransformer.generateCacheName(tmp).endsWith(String.valueOf(tmp.lastModified())));

        tmp = File.createTempFile("asdf", "asdf.s", tmpDir);
        assertTrue(DefaultPluginTransformer.generateCacheName(tmp).endsWith(String.valueOf(".s")));

    }

}
