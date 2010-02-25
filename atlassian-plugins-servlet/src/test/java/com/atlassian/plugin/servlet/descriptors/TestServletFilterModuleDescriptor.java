package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.module.DefaultModuleClassFactory;
import com.atlassian.plugin.module.ModuleCreator;
import junit.framework.TestCase;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import com.mockobjects.dynamic.Mock;

import java.util.Collections;

public class TestServletFilterModuleDescriptor extends TestCase
{
    ServletFilterModuleDescriptor descriptor;

    @Override
    public void setUp()
    {
        descriptor = new ServletFilterModuleDescriptor
                ( new DefaultModuleClassFactory(Collections.<ModuleCreator>emptyList()), (ServletModuleManager) new Mock(ServletModuleManager.class).proxy());
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
        assertEquals(FilterLocation.BEFORE_DISPATCH, descriptor.getLocation());
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
        } catch (PluginParseException ex)
        {
            // very good
        }
    }

    public void testInitWithDetails()
    {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("location", "after-encoding");
        e.addAttribute("weight", "122");
        descriptor.init(plugin, e);
        assertEquals(FilterLocation.AFTER_ENCODING, descriptor.getLocation());
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
        } catch (PluginParseException ex)
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
        } catch (PluginParseException ex)
        {
            // very good
        }
    }
}
