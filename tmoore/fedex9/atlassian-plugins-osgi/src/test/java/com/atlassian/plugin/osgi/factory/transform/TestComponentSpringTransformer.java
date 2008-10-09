package com.atlassian.plugin.osgi.factory.transform;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.IOException;

public class TestComponentSpringTransformer extends TestCase
{
    public void testTransform() throws IOException
    {
        ComponentSpringTransformer transformer = new ComponentSpringTransformer();

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

        // No component
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "count(/atlassian-plugin/child::*)=0");
    }

}