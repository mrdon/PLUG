package com.atlassian.plugin.osgi.factory.transform;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.test.PluginJarBuilder;

public class TestHostComponentSpringTransformer extends TestCase
{
    HostComponentSpringTransformer transformer = new HostComponentSpringTransformer();
    File jar;

    @Override
    public void setUp() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();
    }

    public void testTransform() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", new SomeInterface(){}, SomeInterface.class));
        }}, pluginRoot, "osgi:reference[@id='foo' and @filter='(&(bean-name=foo)(plugins-host=true))']/osgi:interfaces/beans:value/text()='"+SomeInterface.class.getName()+"'");
    }

    public void testTransformWithPoundSign() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo#1", new SomeInterface(){}, SomeInterface.class));
        }}, pluginRoot, "osgi:reference[@id='fooLB1' and @filter='(&(bean-name=foo#1)(plugins-host=true))']/osgi:interfaces/beans:value/text()='"+SomeInterface.class.getName()+"'");
    }

    public void testTransformWithNoBeanName() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration(SomeInterface.class));
        }}, pluginRoot, "osgi:reference[@id='bean0' and not(@filter)]/osgi:interfaces/beans:value/text()='"+SomeInterface.class.getName()+"'");
    }

    public void testTransformNoMatches() throws Exception
    {
        File jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(String bar) {}",
                        "}")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();
        HostComponentSpringTransformer transformer = new HostComponentSpringTransformer();

        // host component with name
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", SomeInterface.class));
        }}, pluginRoot, "not(osgi:reference[@id='foo'])");
    }

    public void testTransformMatchInInnerJar() throws Exception
    {
        File innerJar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .build();
        File jar = new PluginJarBuilder()
                .addFile("META-INF/lib/inner.jar", innerJar)
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n" +
                        "Bundle-ClassPath: .,\n" +
                        "     META-INF/lib/inner.jar\n")
                .build();
        HostComponentSpringTransformer transformer = new HostComponentSpringTransformer();

        // host component with name
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", SomeInterface.class));
        }}, pluginRoot, "osgi:reference[@id='foo']");
    }

    public void testTransformWithExistingComponentImport() throws IOException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element compImport = pluginRoot.addElement("component-import");
        compImport.addAttribute("key", "foo");

        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>(){{
            add(new StubHostComponentRegistration("foo", new SomeInterface(){}, SomeInterface.class));
        }}, pluginRoot, "osgi:reference[@id='foo0' and @filter='(&(bean-name=foo)(plugins-host=true))']/osgi:interfaces/beans:value/text()='"+SomeInterface.class.getName()+"'");
    }
}
