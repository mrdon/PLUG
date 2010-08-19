package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.factory.transform.stage.ComponentImportSpringStage;
import com.atlassian.plugin.osgi.factory.transform.stage.ComponentSpringStage;
import com.atlassian.plugin.osgi.factory.transform.stage.HostComponentSpringStage;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.test.PluginTestUtils;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDefaultPluginTransformer extends TestCase
{
    private static final Logger LOG = LoggerFactory.getLogger(TestDefaultPluginTransformer.class);

    private DefaultPluginTransformer transformer;
    private File tmpDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        tmpDir = PluginTestUtils.createTempDirectory("plugin-transformer");
        transformer = new DefaultPluginTransformer(new DefaultOsgiPersistentCache(tmpDir), SystemExports.NONE, null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
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

    public void testTransformWithBeanConflictBetweenComponentAndHostComponent() throws Exception
    {
        final File file = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  com.atlassian.plugin.osgi.factory.transform.Fooable bar;",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='plugin1' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "<component key='host_component1' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .build();

        try
        {
            transformer.transform(new JarPluginArtifact(file), new ArrayList<HostComponentRegistration>()
            {
                {
                    HostComponentRegistration reg = new StubHostComponentRegistration(Fooable.class);
                    reg.getProperties().put(PropertyBuilder.BEAN_NAME, "host_component1");
                    add(reg);
                }
            });

            fail(PluginTransformationException.class.getSimpleName() + " expected");
        }
        catch (PluginTransformationException e)
        {
            // good, now check the content inside the error message.

            // this check looks weird since it relies on message scraping
            // but without all the information expected here, users would not be able to figure out what went wrong.
            LOG.info(e.toString());
            e.getMessage().contains("host_component1");
            e.getMessage().contains(ComponentSpringStage.BEAN_SOURCE);
            e.getMessage().contains(HostComponentSpringStage.BEAN_SOURCE);
        }
    }

    public void testTransformWithBeanConflictBetweenComponentAndImportComponent() throws Exception
    {
        final File file = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  com.atlassian.plugin.osgi.factory.transform.Fooable bar;",
                        "}")
                .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                                  "package com.atlassian.plugin.osgi;",
                                  "public interface SomeInterface {}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='plugin1' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "<component key='component1' class='my.Foo'/>",
                        "<component-import key='component1'>",
                        "    <interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                        "</component-import>",
                        "</atlassian-plugin>")
                .build();

        try
        {
            transformer.transform(new JarPluginArtifact(file), new ArrayList<HostComponentRegistration>());
            fail(PluginTransformationException.class.getSimpleName() + " expected");
        }
        catch (PluginTransformationException e)
        {
            // good, now check the content inside the error message.

            // this check looks weird since it relies on message scraping
            // but without all the information expected here, users would not be able to figure out what went wrong.
            LOG.info(e.toString());
            e.getMessage().contains("component1");
            e.getMessage().contains(ComponentSpringStage.BEAN_SOURCE);
            e.getMessage().contains(ComponentImportSpringStage.BEAN_SOURCE);
        }
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
