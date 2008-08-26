package com.atlassian.plugin.osgi.loader;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.dom4j.tree.DefaultElement;

public class TestFilteringXmlDescriptorParser extends TestCase
{
    public void testCreateModuleDescriptor() throws PluginParseException
    {
        FilteringXmlDescriptorParser parser = new FilteringXmlDescriptorParser(new ByteArrayInputStream("<foo/>".getBytes()), "foo");

        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expect("init", C.ANY_ARGS);
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.expectAndReturn("getModuleDescriptor", C.args(C.eq("bar")), mockModuleDescriptor.proxy());
        assertNull(parser.createModuleDescriptor(null, new DefaultElement("foo"), null));
        assertNotNull(parser.createModuleDescriptor(null, new DefaultElement("bar"), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy()));

        mockModuleDescriptor.verify();
        mockModuleDescriptorFactory.verify();
    }
}
