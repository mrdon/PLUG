package com.atlassian.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.DefaultPluginManager;

public class TestComponentImportSpringStage extends TestCase
{
    public void testTransform() throws IOException, DocumentException
    {
        ComponentImportSpringStage stage = new ComponentImportSpringStage();

        // interface as attribute
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo' and @interface='my.Foo']");

        // interface as element
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.IFoo'");


    }

    public void testTransformImportEvenUnusedPackages() throws Exception, DocumentException
    {
        ComponentImportSpringStage stage = new ComponentImportSpringStage();
        File jar = new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foo' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "</atlassian-plugin>")
                .build();

        TransformContext context = new TransformContext(null, jar, DefaultPluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        stage.execute(context);
        assertTrue(context.getExtraImports().contains(SomeInterface.class.getPackage().getName()));

    }
}
