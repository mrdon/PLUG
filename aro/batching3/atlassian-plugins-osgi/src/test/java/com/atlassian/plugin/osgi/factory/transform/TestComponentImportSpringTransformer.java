package com.atlassian.plugin.osgi.factory.transform;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;

public class TestComponentImportSpringTransformer extends TestCase
{
    public void testTransform() throws IOException
    {
        ComponentImportSpringTransformer transformer = new ComponentImportSpringTransformer();

        // interface as attribute
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "osgi:reference[@id='foo' and @interface='my.Foo']");

        // interface as element
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.IFoo'");


    }

}