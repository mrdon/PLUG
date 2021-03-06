package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import junit.framework.TestCase;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

public class TestOsgiPluginXmlDescriptorParser extends TestCase
{

    public void testCreateModuleDescriptor() throws PluginParseException, IllegalAccessException, ClassNotFoundException, InstantiationException
    {
        OsgiPluginXmlDescriptorParser parser = new OsgiPluginXmlDescriptorParser(new ByteArrayInputStream("<foo/>".getBytes()), null);

        ModuleDescriptor desc = mock(ModuleDescriptor.class);
        when(desc.getKey()).thenReturn("foo");
        ModuleDescriptorFactory factory = mock(ModuleDescriptorFactory.class);
        when(factory.getModuleDescriptor("foo")).thenReturn(desc);

        OsgiPlugin plugin = mock(OsgiPlugin.class);
        Element fooElement = new DefaultElement("foo");
        fooElement.addAttribute("key", "bob");
        assertNotNull(parser.createModuleDescriptor(plugin, fooElement,factory));
        verify(plugin).addModuleDescriptorElement("foo", fooElement);
    }

    public void testFoo() {}
}
