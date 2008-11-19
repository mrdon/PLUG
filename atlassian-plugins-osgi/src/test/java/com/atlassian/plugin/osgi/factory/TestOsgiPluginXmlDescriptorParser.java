package com.atlassian.plugin.osgi.factory;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.dom4j.tree.DefaultElement;
import org.osgi.framework.Bundle;

public class TestOsgiPluginXmlDescriptorParser extends TestCase
{
    public void testCreateModuleDescriptor() throws PluginParseException
    {
        OsgiPluginXmlDescriptorParser parser = new OsgiPluginXmlDescriptorParser(new ByteArrayInputStream("<foo/>".getBytes()), "foo");

        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expect("init", C.ANY_ARGS);
        mockModuleDescriptor.expectAndReturn("getKey", "bob");
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.expectAndReturn("getModuleDescriptor", C.args(C.eq("bar")), mockModuleDescriptor.proxy());
        Mock mockBundle = new Mock(Bundle.class);
        assertNull(parser.createModuleDescriptor(new OsgiPlugin((Bundle) mockBundle.proxy()), new DefaultElement("foo"), null));
        assertNotNull(parser.createModuleDescriptor(new OsgiPlugin((Bundle) mockBundle.proxy()), new DefaultElement("bar"), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy()));

        mockModuleDescriptor.verify();
        mockModuleDescriptorFactory.verify();
    }
}
