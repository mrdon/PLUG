package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.dom4j.tree.DefaultElement;
import org.dom4j.Element;
import org.osgi.framework.Bundle;

public class TestOsgiPluginXmlDescriptorParser extends TestCase
{
    public void testCreateModuleDescriptor() throws PluginParseException
    {
        OsgiPluginXmlDescriptorParser parser = new OsgiPluginXmlDescriptorParser(new ByteArrayInputStream("<foo/>".getBytes()), null);

        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expect("init", C.ANY_ARGS);
        mockModuleDescriptor.expectAndReturn("getKey", "bob");
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptor", C.args(C.eq("foo")), mockModuleDescriptor.proxy());
        Mock mockBundle = new Mock(Bundle.class);

        OsgiPlugin plugin = new OsgiPlugin((Bundle) mockBundle.proxy());
        Element fooElement = new DefaultElement("foo");
        fooElement.addAttribute("key", "bob");
        assertNotNull(parser.createModuleDescriptor(plugin, fooElement,
                (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy()));
        assertEquals(fooElement, plugin.getModuleElements().get("bob"));
        mockModuleDescriptor.verify();
        mockModuleDescriptorFactory.verify();
    }
}
