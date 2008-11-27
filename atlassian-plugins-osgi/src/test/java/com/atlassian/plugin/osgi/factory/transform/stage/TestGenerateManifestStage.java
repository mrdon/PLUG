package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.osgi.framework.Constants;

import javax.print.attribute.AttributeSet;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class TestGenerateManifestStage extends TestCase
{
    private GenerateManifestStage stage;

    @Override
    public void setUp()
    {
        stage = new GenerateManifestStage();
    }
    public void testGenerateManifest() throws Exception
    {
        File file = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin key='com.atlassian.plugins.example' name='Example Plugin'>",
                        "  <plugin-info>",
                        "    <description>",
                        "      A sample plugin for demonstrating the file format.",
                        "    </description>",
                        "    <version>1.1</version>",
                        "    <vendor name='Atlassian Software Systems Pty Ltd' url='http://www.atlassian.com'/>",
                        "  </plugin-info>",
                        "</atlassian-plugin>")
                .addFormattedJava("com.mycompany.myapp.Foo", "package com.mycompany.myapp; public class Foo {}")
                .build();

        TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), file, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);

        Attributes attrs = executeStage(context);

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugins.example", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("A sample plugin for demonstrating the file format.", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));
        assertEquals(".", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        assertEquals("*;timeout:=60", attrs.getValue("Spring-Context"));

    }

    public void testGenerateManifestWithProperInferredImports() throws URISyntaxException, IOException, PluginParseException
    {
        final File file = new PluginJarBuilder()
                .addPluginInformation("someKey", "someName", "1.0")
                .build();
        TransformContext context = new TransformContext(null, file, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        context.getExtraImports().add(AttributeSet.class.getPackage().getName());
        Attributes attrs = executeStage(context);

        assertTrue(attrs.getValue(Constants.IMPORT_PACKAGE).contains(AttributeSet.class.getPackage().getName()));

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

        TransformContext context = new TransformContext(null, plugin, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        context.getExtraImports().add(AttributeSet.class.getPackage().getName());
        Attributes attrs = executeStage(context);
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

        TransformContext context = new TransformContext(null, plugin, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        Attributes attrs = executeStage(context);

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

        TransformContext context = new TransformContext(null, plugin, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        Attributes attrs = executeStage(context);

        assertEquals("1.0", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("innerjarcp", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(2, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar.jar"));

        final Collection imports = Arrays.asList(attrs.getValue("Import-Package").split(","));
        assertEquals(3, imports.size());
        assertTrue(imports.contains(Logger.class.getPackage().getName()+";resolution:=optional"));
        assertTrue(imports.contains(Filter.class.getPackage().getName()+";resolution:=optional"));
    }

    public void testGenerateManifestWithBundleInstructions() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("test.plugin", "test.plugin", "1.0")
                .addJava("foo.MyClass", "package foo; public class MyClass{}")
                .addJava("foo.internal.MyPrivateClass", "package foo.internal; public class MyPrivateClass{}")
                .build();

        TransformContext context = new TransformContext(null, plugin, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        context.getBndInstructions().put("Export-Package", "!*.internal.*,*");
        Attributes attrs = executeStage(context);
        assertEquals("test.plugin", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("foo", attrs.getValue(Constants.EXPORT_PACKAGE));
    }

    private Attributes executeStage(TransformContext context) throws IOException
    {
        stage.execute(context);
        Manifest mf = new Manifest(new ByteArrayInputStream(context.getFileOverrides().get("META-INF/MANIFEST.MF")));
        Attributes attrs = mf.getMainAttributes();
        return attrs;
    }
}
