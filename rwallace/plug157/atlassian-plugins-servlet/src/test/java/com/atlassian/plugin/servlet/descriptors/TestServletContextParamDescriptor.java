package com.atlassian.plugin.servlet.descriptors;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.descriptors.TestServletModuleDescriptor.SomeServlet;

public class TestServletContextParamDescriptor extends TestCase
{
    ServletContextParamDescriptor descriptor;

    @Override
    public void setUp()
    {
        descriptor = new ServletContextParamDescriptor();
    }

    @Override
    public void tearDown()
    {
        descriptor = null;
    }

    public void testInit() 
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        descriptor.init(plugin, e);
    }

    private Element getValidConfig()
    {
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramName = new DOMElement("param-name");
        paramName.setText("test.param.name");
        e.add(paramName);
        Element paramValue = new DOMElement("param-value");
        paramValue.setText("test.param.value");
        e.add(paramValue);
        return e;
    }

    public void testInitWithNoParamName() 
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramValue = new DOMElement("param-value");
        paramValue.setText("test.param.value");
        e.add(paramValue);
        try
        {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // very good
        }
    }

    public void testInitWithNoParamValue() 
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramName = new DOMElement("param-name");
        paramName.setText("test.param.name");
        e.add(paramName);
        try
        {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // very good
        }
    }
}
