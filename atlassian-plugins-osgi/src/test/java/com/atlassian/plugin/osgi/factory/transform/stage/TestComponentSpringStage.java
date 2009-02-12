package com.atlassian.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;
import java.io.File;
import java.io.ByteArrayInputStream;

import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

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
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']");

        // public component, no interface
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                                                                       "osgi:service[@id='foo_osgiService' and @ref='foo']");

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
        TransformContext ctx = new TransformContext(null, null, (PluginArtifact) mockPluginArtifact.proxy(), "foo");
        transformer.execute(ctx);

        assertTrue(ctx.getExtraExports().contains("my"));
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
        TransformContext ctx = new TransformContext(null, null, (PluginArtifact) mockPluginArtifact.proxy(), "foo");
        transformer.execute(ctx);

        assertFalse(ctx.getExtraExports().contains("my"));
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
        TransformContext ctx = new TransformContext(null, null, (PluginArtifact) mockPluginArtifact.proxy(), "foo");
        ctx.getExtraExports().add("my");
        transformer.execute(ctx);

        assertEquals(ctx.getExtraExports().indexOf("my"), ctx.getExtraExports().lastIndexOf("my"));
        mockPluginArtifact.verify();
    }
}
