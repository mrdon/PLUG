package com.atlassian.plugin.servlet.descriptors;

import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.ServletModuleManager;

public class TestServletModuleDescriptor extends TestCase
{
    ServletModuleDescriptor descriptor;

    @Override
    public void setUp()
    {
        descriptor = new ServletModuleDescriptor()
        {
            protected void autowireObject(Object obj) {throw new UnsupportedOperationException(); }
            protected ServletModuleManager getServletModuleManager() { return null; }
        };
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
        Element e = new DOMElement("servlet");
        e.addAttribute("key", "key2");
        e.addAttribute("class", SomeServlet.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);
        return e;
    }

    public void testInitWithNoUrlPattern() 
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet");
        e.addAttribute("key", "key2");
        e.addAttribute("class", SomeServlet.class.getName());
        try
        {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // very good
        }
    }
    
    static class SomeServlet extends HttpServlet {}
}
