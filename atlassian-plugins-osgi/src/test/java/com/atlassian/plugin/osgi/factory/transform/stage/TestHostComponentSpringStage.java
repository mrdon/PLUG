package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.osgi.factory.transform.*;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.factory.transform.test.SomeClass;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableModel;
import javax.servlet.Servlet;

import junit.framework.TestCase;

public class TestHostComponentSpringStage extends TestCase
{

    private HostComponentSpringStage transformer = new HostComponentSpringStage();
    private File jar;
    private SystemExports systemExports;

    @Override
    public void setUp() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .addResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0\n" +
                    "Bundle-Version: 1.0\n" +
                    "Bundle-SymbolicName: my.server\n" +
                    "Bundle-ManifestVersion: 2\n")
                .build();
        systemExports = new SystemExports("javax.servlet;version=\"2.3\",javax.servlet.http;version=\"2.3\"");
    }

    public void testTransform() throws IOException, DocumentException
    {
        SpringTransformerTestHelper.transform(
            transformer,
            jar,
            new ArrayList<HostComponentRegistration>()
            {
                {
                    add(new StubHostComponentRegistration("foo", new SomeInterface()
                    {}, SomeInterface.class));
                }
            },
            null,
            "beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'");
    }

    public void testTransformWithProperNestedInferredImports() throws Exception
    {
        jar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {",
            "  public Foo(javax.swing.table.TableModel bar) {}", "}").addPluginInformation("my.plugin", "my.plugin", "1.0").build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>()
        {
            {
                add(new MockRegistration("foo", TableModel.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains("javax.swing.event"));

    }

    public void testTransformWithProperNestedVersionedInferredImports() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(javax.servlet.Servlet servlet) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>()
        {
            {
                add(new MockRegistration("foo", Servlet.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains("javax.servlet;version=\"[2.3,2.3]\""));

    }

    public void testTransformWithInferredImportsOfSuperInterfaces() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.factory.transform.FooChild bar) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>()
        {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains(SomeClass.class.getPackage().getName()));

    }

    public void testTransformWithSuperClassThatUsesHostComponent() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo extends " + AbstractFoo.class.getName() + " {",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>()
        {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains(FooChild.class.getPackage().getName()));
    }

    public void testTransformWithSuperClassInJar() throws Exception
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "}")
                .addFormattedJava("my2.Bar",
                        "package my2;",
                        "public class Bar extends my.Foo {",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>()
        {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME);
        transformer.execute(context);
        assertEquals(0, context.getExtraImports().size());

    }

    public void testTransformWithPoundSign() throws IOException, DocumentException
    {
        SpringTransformerTestHelper.transform(
            transformer,
            jar,
            new ArrayList<HostComponentRegistration>()
            {
                {
                    add(new StubHostComponentRegistration("foo#1", new SomeInterface()
                    {}, SomeInterface.class));
                }
            },
            null,
            "beans:bean[@id='fooLB1']/beans:property[@name='filter']/@value='(&(bean-name=foo#1)(plugins-host=true))'");
    }

    public void testTransformNoMatches() throws Exception
    {
        final File jar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {", "  public Foo(String bar) {}", "}").addPluginInformation(
            "my.plugin", "my.plugin", "1.0").addResource("META-INF/MANIFEST.MF",
            "Manifest-Version: 1.0\n" + "Bundle-Version: 1.0\n" + "Bundle-SymbolicName: my.server\n" + "Bundle-ManifestVersion: 2\n").build();

        // host component with name
        assertNull(SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>()
        {
            {
                add(new StubHostComponentRegistration("foo", SomeInterface.class));
            }
        }, null, "not(beans:bean[@id='foo'])"));
    }

    public void testTransformMatchInInnerJar() throws Exception
    {
        final File innerJar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {",
            "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}", "}").build();
        final File jar = new PluginJarBuilder().addFile("META-INF/lib/inner.jar", innerJar).addResource(
            "META-INF/MANIFEST.MF",
            "Manifest-Version: 1.0\n" + "Bundle-Version: 1.0\n" + "Bundle-SymbolicName: my.server\n" + "Bundle-ManifestVersion: 2\n" + "Bundle-ClassPath: .,\n" + "     META-INF/lib/inner.jar\n").addPluginInformation(
            "my.plugin", "my.plugin", "1.0").build();

        // host component with name
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>()
        {
            {
                add(new StubHostComponentRegistration("foo", SomeInterface.class));
            }
        }, null, "beans:bean[@id='foo']");
    }

    public void testTransformWithExistingComponentImportName() throws Exception, DocumentException
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foo' class='Foo' interface='Foo'/>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
            transformer,
            jar,
            new ArrayList<HostComponentRegistration>()
            {
                {
                    assertTrue(add(new StubHostComponentRegistration("foo", new SomeInterface()
                    {}, SomeInterface.class)));
                }
            },
            null,
            "beans:bean[@id='foo0']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'");
    }

    public void testTransformWithExistingComponentImportInterface() throws Exception, DocumentException
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foobar'>",
                        "    <interface>com.atlassian.plugin.osgi.SomeInterface</interface>",
                        "  </component-import>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
            transformer,
            jar,
            new ArrayList<HostComponentRegistration>()
            {
                {
                    assertTrue(add(new StubHostComponentRegistration("foo", new SomeInterface()
                    {}, SomeInterface.class)));
                }
            },
            null,
            "not(beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))']");
    }

    public void testTransformWithExistingComponentImportInterfacePartialMatch() throws Exception, DocumentException
    {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(com.atlassian.plugin.osgi.factory.transform.Barable bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foobar'>",
                        "    <interface>com.atlassian.plugin.osgi.factory.transform.Barable</interface>",
                        "  </component-import>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                        "Bundle-Version: 1.0\n" +
                        "Bundle-SymbolicName: my.server\n" +
                        "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
            transformer,
            jar,
            new ArrayList<HostComponentRegistration>()
            {
                {
                    assertTrue(add(new StubHostComponentRegistration("foo", new Fooable()
                    {
                        public SomeClass getSomeClass()
                        {
                            return null;  //To change body of implemented methods use File | Settings | File Templates.
                        }
                    }, Barable.class, Fooable.class)));
                }
            },
            null,
            "not(beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))']");
    }

}
