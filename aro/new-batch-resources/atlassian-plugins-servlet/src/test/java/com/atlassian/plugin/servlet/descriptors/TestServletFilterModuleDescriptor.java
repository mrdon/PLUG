package com.atlassian.plugin.servlet.descriptors;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.FilterTestUtils.FilterAdapter;

public class TestServletFilterModuleDescriptor extends TestCase
{
    ServletFilterModuleDescriptor descriptor;

    @Override
    public void setUp()
    {
        descriptor = new ServletFilterModuleDescriptor()
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
        assertEquals(FilterLocation.bottom, descriptor.getLocation());
        assertEquals(100, descriptor.getWeight());
    }

    private Element getValidConfig()
    {
        Element e = new DOMElement("servlet-filter");
        e.addAttribute("key", "key2");
        e.addAttribute("class", FilterAdapter.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);
        return e;
    }

    public void testInitWithNoUrlPattern() 
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-filter");
        e.addAttribute("key", "key2");
        e.addAttribute("class", FilterAdapter.class.getName());
        try
        {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // very good
        }
    }

    public void testInitWithDetails()
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("location", "top");
        e.addAttribute("weight", "122");
        descriptor.init(plugin, e);
        assertEquals(FilterLocation.top, descriptor.getLocation());
        assertEquals(122, descriptor.getWeight());
    }

    public void testInitWithBadLocation()
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("location", "t23op");
        try
        {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex)
        {
            // very good
        }
    }

    public void testInitWithBadWeight()
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("weight", "t23op");
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
