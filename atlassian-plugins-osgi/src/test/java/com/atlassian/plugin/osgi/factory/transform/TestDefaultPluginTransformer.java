package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.osgi.factory.transform.test.SomeClass;
import com.atlassian.plugin.test.PluginTestUtils;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.dom4j.DocumentException;
import org.osgi.framework.Constants;

import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.TableModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TestDefaultPluginTransformer extends TestCase
{
    public void testGenerateManifest() throws URISyntaxException, IOException, PluginParseException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(file).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), file, null);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugin.sample", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("This is a brief textual description of the plugin", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));
        assertEquals(".", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        assertEquals("*;timeout=60", attrs.getValue("Spring-Context"));
        
    }

    public void testGenerateManifestWithProperInferredImports() throws URISyntaxException, IOException, PluginParseException
    {
        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration(new HashAttributeSet(), AttributeSet.class));
        }};
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(file).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), file, regs);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertTrue(attrs.getValue(Constants.IMPORT_PACKAGE).contains(AttributeSet.class.getPackage().getName()));

    }

    public void testGenerateManifestWithProperNestedInferredImports() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration("foo", TableModel.class));
        }};

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, regs);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        String importPackage = attrs.getValue(Constants.IMPORT_PACKAGE);
        assertTrue(attrs.getValue(Constants.IMPORT_PACKAGE).contains("javax.swing.event,"));

    }

    public void testGenerateManifestWithInferredImportsOfSuperInterfaces() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration("foo", FooChild.class));
        }};

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, regs);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        String importPackage = attrs.getValue(Constants.IMPORT_PACKAGE);
        System.out.println("imports:"+importPackage);
        assertTrue(attrs.getValue(Constants.IMPORT_PACKAGE).contains(SomeClass.class.getPackage().getName()));

    }

    public void testGenerateManifestMergeHostComponentImportsWithExisting() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ClassPath: .,foo\n")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .addResource("foo/bar.txt", "Something")
                .build();

        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration("foo", AttributeSet.class));
        }};

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, regs);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();
        assertEquals("my.foo.symbolicName", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals(".,foo", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        String importPackage = attrs.getValue(Constants.IMPORT_PACKAGE);
        System.out.println("imports:"+importPackage);
        assertTrue(importPackage.contains("javax.print.attribute,"));
        assertTrue(importPackage.contains("javax.swing"));

    }

    public void testGenerateManifestWithBundleInstructions() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions>",
                        "            <Export-Package>!*.internal.*,*</Export-Package>",
                        "        </bundle-instructions>",
                        "    </plugin-info>",
                        "</atlassian-plugin>")
                .addJava("foo.MyClass", "package foo; public class MyClass{}")
                .addJava("foo.internal.MyPrivateClass", "package foo.internal; public class MyPrivateClass{}")
                .build();

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, Collections.<HostComponentRegistration>emptyList());
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();
        assertEquals("test.plugin", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("foo", attrs.getValue(Constants.EXPORT_PACKAGE));
    }

    public void testGenerateManifestUsingPluginInfoParameters() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addResource("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n" +
                        "Import-Package: javax.swing\n" +
                        "Bundle-SymbolicName: my.foo.symbolicName\n" +
                        "Bundle-ClassPath: .,foo\n")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .addResource("foo/bar.txt", "Something")
                .build();

        List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {{
            add(new MockRegistration("foo", AttributeSet.class));
        }};

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, regs);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();
        assertEquals("my.foo.symbolicName", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals(".,foo", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        String importPackage = attrs.getValue(Constants.IMPORT_PACKAGE);
        System.out.println("imports:"+importPackage);
        assertTrue(importPackage.contains("javax.print.attribute,"));
        assertTrue(importPackage.contains("javax.swing"));

    }

    public void testGenerateManifest_innerjars() throws URISyntaxException, PluginParseException, IOException
    {
        File innerJar = new PluginJarBuilder("innerjar1")
                .build();
        File innerJar2 = new PluginJarBuilder("innerjar2")
                .build();
        File plugin = new PluginJarBuilder("plugin")
                .addFile("META-INF/lib/innerjar.jar", innerJar)
                .addFile("META-INF/lib/innerjar2.jar", innerJar2)
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(getClassLoader(plugin).getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, null);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(3, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar.jar"));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar2.jar"));
    }

    public void testGenerateManifest_innerjarsInImports() throws Exception, PluginParseException, IOException
    {
        File innerJar = new PluginJarBuilder("innerjar")
                .addJava("my.Foo", "package my;import org.apache.log4j.Logger; public class Foo{Logger log;}")
                .build();
        assertNotNull(innerJar);
        File plugin = new PluginJarBuilder("plugin")
                .addJava("my.Bar", "package my;import org.apache.log4j.spi.Filter; public class Bar{Filter log;}")
                .addFile("META-INF/lib/innerjar.jar", innerJar)
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        URLClassLoader cl = new URLClassLoader(new URL[]{plugin.toURI().toURL()}, null);
        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        byte[] manifest = transformer.generateManifest(cl.getResourceAsStream(PluginManager.PLUGIN_DESCRIPTOR_FILENAME), plugin, null);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertEquals("1.0", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("innerjarcp", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(2, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar.jar"));

        final Collection imports = Arrays.asList(attrs.getValue("Import-Package").split(","));
        assertEquals(5, imports.size());
        assertTrue(imports.contains(Logger.class.getPackage().getName()+";resolution:=optional"));
        assertTrue(imports.contains(Filter.class.getPackage().getName()+";resolution:=optional"));
    }

    public void testAddFilesToZip() throws URISyntaxException, IOException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

        Map<String,byte[]> files = new HashMap<String, byte[]>() {{
            put("foo", "bar".getBytes());
        }};
        File copy = DefaultPluginTransformer.addFilesToExistingZip(file, files);
        assertNotNull(copy);
        assertTrue(!copy.getName().equals(file.getName()));
        assertTrue(copy.length() != file.length());

        ZipFile zip = new ZipFile(copy);
        ZipEntry entry = zip.getEntry("foo");
        assertNotNull(entry);
    }

    public void testTransform() throws URISyntaxException, IOException, PluginParseException, DocumentException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin2.jar");

        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        File copy = transformer.transform(file, new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(String.class));
        }});

        assertNotNull(copy);
        final JarFile jar = new JarFile(copy);
        final Attributes attrs = jar.getManifest().getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugin.sample", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("This is a brief textual description of the plugin", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));

        assertNotNull(jar.getEntry(DefaultPluginTransformer.ATLASSIAN_PLUGIN_SPRING_XML));

    }

    private ClassLoader getClassLoader(File file) throws MalformedURLException
    {
        return new URLClassLoader(new URL[]{file.toURL()}, null);
    }

}
