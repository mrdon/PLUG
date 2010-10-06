package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Set;

import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.util.validation.ValidationException;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import static org.mockito.Mockito.*;

public class TestComponentSpringStage extends TestCase
{
    public void testTransform() throws IOException, DocumentException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();

        // private component
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("alias", "hohoho");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                                                                       "beans:alias[@name='foo' and @alias='hohoho']");

        // public component, interface
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                                                                       "osgi:service[@id='foo_osgiService' and @ref='foo']",
                                                                       "//osgi:interfaces",
                                                                       "//beans:value[.='my.IFoo']");

        // public component, interface as attribute
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("interface", "my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                                                                       "osgi:service[@id='foo_osgiService' and @ref='foo']",
                                                                       "//osgi:interfaces",
                                                                       "//beans:value[.='my.IFoo']");

    }

    public void testTransformWithServiceProperties() throws IOException, DocumentException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();

        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("interface", "my.IFoo");

        Element svcprops = component.addElement("service-properties");
        Element prop = svcprops.addElement("entry");
        prop.addAttribute("key", "foo");
        prop.addAttribute("value", "bar");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                                                                       "osgi:service[@id='foo_osgiService']/osgi:service-properties",
                                                                       "osgi:service[@id='foo_osgiService']/osgi:service-properties/beans:entry[@key='foo' and @value='bar']",
                                                                       "//osgi:interfaces",
                                                                       "//beans:value[.='my.IFoo']");

        svcprops.clearContent();
        try
        {
            SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                    "osgi:service[@id='foo_osgiService']/osgi:service-properties",
                    "//osgi:interfaces",
                    "//beans:value[.='my.IFoo']");
            fail("Validation exception should have been thrown");
        }
        catch (ValidationException ex)
        {
            // expected
        }
    }

    public void testTransformForOneApp() throws IOException, DocumentException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();

        // public component, interface
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("application", "bar");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "not(beans:bean[@id='foo' and @class='my.Foo'])");

        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("application", "foo");
        inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']");

    }

    public void testExportsAdded() throws IOException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        mockPluginArtifact.expectAndReturn("doesResourceExist", C.args(C.eq("my/IFoo.class")), true);
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        transformer.execute(ctx);

        assertTrue(ctx.getExtraExports().contains("my"));

        // the generated bean should be tracked.
        assertTrue(ctx.beanExists("foo"));

        mockPluginArtifact.verify();
    }

    public void testExportsNotInJar() throws IOException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        mockPluginArtifact.expectAndReturn("doesResourceExist", C.args(C.eq("my/IFoo.class")), false);
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        transformer.execute(ctx);

        assertFalse(ctx.getExtraExports().contains("my"));
        // the generated bean should be tracked.
        assertTrue(ctx.beanExists("foo"));

        mockPluginArtifact.verify();
    }

    public void testExportsExist() throws IOException
    {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        ctx.getExtraExports().add("my");
        transformer.execute(ctx);

        assertEquals(ctx.getExtraExports().indexOf("my"), ctx.getExtraExports().lastIndexOf("my"));
        // the generated bean should be tracked.
        assertTrue(ctx.beanExists("foo"));

        mockPluginArtifact.verify();
    }

    public void testImportManifestGenerationOnInterfaces() throws Exception
    {
        final ComponentSpringStage stage = new ComponentSpringStage();

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
                        "       <interface>my2.MyFooInterface</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .build();

        ServiceReference serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { "my.Service"});

        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[] {serviceReference});

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(pluginJar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);

        // don't import local interface.
        assertFalse(context.getExtraImports().contains("my2.MyFooInterface"));

        // import only interfaces that don't exist in the plugin itself.
        assertTrue(context.getExtraImports().contains("com.atlassian.plugin.osgi.factory.transform.dummypackage1"));
        assertTrue(context.getExtraImports().contains("com.atlassian.plugin.osgi.factory.transform.dummypackage0"));
    }
}