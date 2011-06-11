package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.factory.transform.stage.ComponentImportSpringStage;
import com.atlassian.plugin.osgi.factory.transform.stage.ComponentSpringStage;
import com.atlassian.plugin.osgi.factory.transform.stage.HostComponentSpringStage;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.test.PluginTestUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        final File copy = transformer.transform(new JarPluginArtifact(new DeploymentUnit(file)), new ArrayList<HostComponentRegistration>()
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

    // TODO: turn this back on PLUG-682
    public void notTestTransformWithBeanConflictBetweenComponentAndHostComponent() throws Exception
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
            transformer.transform(new JarPluginArtifact(new DeploymentUnit(file)), new ArrayList<HostComponentRegistration>()
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

    // TODO: turn this back on PLUG-682
    public void notTestTransformWithBeanConflictBetweenComponentAndImportComponent() throws Exception
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
            transformer.transform(new JarPluginArtifact(new DeploymentUnit(file)), new ArrayList<HostComponentRegistration>());
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

    public void testImportManifestGenerationOnInterfaces() throws Exception
    {
        final File innerJar = new PluginJarBuilder()
                .addFormattedJava("my.innerpackage.InnerPackageInterface1",
                        "package my.innerpackage;",
                        "public interface InnerPackageInterface1 {}")
                .build();

        final File pluginJar = new PluginJarBuilder()
                .addFormattedJava("my.MyFooChild",
                        "package my;",
                        "public class MyFooChild extends com.atlassian.plugin.osgi.factory.transform.dummypackage2.DummyClass2 {",
                        "}")
                .addFormattedJava("my2.MyFooInterface",
                        "package my2;",
                        "public interface MyFooInterface {}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='plugin1' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='component1' class='my.MyFooChild' public='true'>",
                        "       <interface>com.atlassian.plugin.osgi.factory.transform.dummypackage0.DummyInterface0</interface>",
                        "       <interface>com.atlassian.plugin.osgi.factory.transform.dummypackage1.DummyInterface1</interface>",
                        "       <interface>my.innerpackage.InnerPackageInterface1</interface>",
                        "       <interface>my2.MyFooInterface</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFile("META-INF/lib/mylib.jar", innerJar)
                .build();

        File outputFile = transformer.transform(new JarPluginArtifact(new DeploymentUnit(pluginJar)), new ArrayList<HostComponentRegistration>());

        JarFile outputJar = new JarFile(outputFile);
        String importString = outputJar.getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE);

        // this should be done by binary scanning.
        assertTrue(importString.contains("com.atlassian.plugin.osgi.factory.transform.dummypackage2"));

        // referred to by interface declaration.
        assertTrue(importString.contains("com.atlassian.plugin.osgi.factory.transform.dummypackage1"));

        // referred to by interface declaration
        assertTrue(importString.contains("com.atlassian.plugin.osgi.factory.transform.dummypackage0"));

        // should not import an interface which exists in plugin itself.
        assertFalse(importString.contains("my2.MyFooInterface"));

        // should not import an interface which exists in inner jar.
        assertFalse(importString.contains("my.innerpackage"));
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

    public void testTransformComponentMustNotPerformKeyConversion() throws Exception
    {
        File outputJarFile = runTransform();

        assertBeanNames(outputJarFile, "META-INF/spring/atlassian-plugins-components.xml",
                        ImmutableMap.<String, String>builder().put("beans", "http://www.springframework.org/schema/beans").build(),
                        "//beans:bean",
                        Sets.newHashSet("TESTING1", "testing2"));
    }

    public void testTransformImportMustNotPerformKeyConversion() throws Exception
    {
        File outputJarFile = runTransform();

        assertBeanNames(outputJarFile, "META-INF/spring/atlassian-plugins-component-imports.xml",
                        ImmutableMap.<String, String>builder().put("osgi", "http://www.springframework.org/schema/osgi").build(),
                        "//osgi:reference",
                        Sets.newHashSet("TESTING3", "testing4"));
    }

    public void testTransformHostComponentMustNotPerformKeyConversion() throws Exception
    {
        File outputJarFile = runTransform();

        assertBeanNames(outputJarFile, "META-INF/spring/atlassian-plugins-host-components.xml",
                        ImmutableMap.<String, String>builder().put("beans", "http://www.springframework.org/schema/beans").build(),
                        "//beans:bean",
                        Sets.newHashSet("TESTING5", "testing6"));
    }

    private File runTransform() throws Exception
    {
        final File file = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import com.atlassian.plugin.osgi.factory.transform.Fooable;",
                        "import com.atlassian.plugin.osgi.factory.transform.FooChild;",
                        "public class Foo {",
                        "  private Fooable bar;",
                        "  public Foo(Fooable bar, FooChild child) { this.bar = bar;} ",
                        "}")
                .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                                  "package com.atlassian.plugin.osgi;",
                                  "public interface SomeInterface {}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='plugin1' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "   <component key='TESTING1' class='my.Foo'/>",
                        "   <component key='testing2' class='my.Foo'/>",
                        "   <component-import key='TESTING3'>",
                        "       <interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                        "   </component-import>",
                        "   <component-import key='testing4'>",
                        "       <interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                        "   </component-import>",
                        "</atlassian-plugin>")
                .build();

        MockRegistration mockReg1 = new MockRegistration(new Foo(), Fooable.class);
        mockReg1.getProperties().put(PropertyBuilder.BEAN_NAME, "TESTING5");
        MockRegistration mockReg2 = new MockRegistration(new FooChild(), FooChild.class);
        mockReg2.getProperties().put(PropertyBuilder.BEAN_NAME, "testing6");

        return transformer.transform(new JarPluginArtifact(new DeploymentUnit(file)), Arrays.<HostComponentRegistration>asList(mockReg1, mockReg2));
    }

    private void assertBeanNames(File outputJarFile, String springFileLocation,
                                 Map<String, String> namespaces, String xpathQuery,
                                 Set<String> expectedIds) throws Exception
    {
        JarFile jarFile = new JarFile(outputJarFile);
        InputStream inputStream = jarFile.getInputStream(jarFile.getEntry(springFileLocation));

        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(inputStream);

        XPath xpath = new Dom4jXPath(xpathQuery);
        xpath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        Set<String> foundBeanNames = new HashSet<String>();
        List<Element> elems = xpath.selectNodes(document);
        for(Element elem:elems)
        {
            foundBeanNames.add(elem.attribute("id").getValue());
        }

        assertEquals(expectedIds, foundBeanNames);
    }
}