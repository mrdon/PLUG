/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:53:39 PM
 */
package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.elements.ResourceDescriptor;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.util.List;

public class TestResourcedModuleDescriptor extends TestCase
{
    public void testGetResourceDescriptor() throws DocumentException, PluginParseException
    {
        ResourcedModuleDescriptor descriptor = makeResourceModuleDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "</animal>").getRootElement());

        assertNull(descriptor.getResourceDescriptor("foo", "bar"));
        assertNull(descriptor.getResourceDescriptor("velocity", "bar"));
        assertNull(descriptor.getResourceDescriptor("foo", "view"));
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()), descriptor.getResourceDescriptor("velocity", "view"));
    }

    public void testGetResourceDescriptorByType() throws DocumentException, PluginParseException
    {
        ResourcedModuleDescriptor descriptor = makeResourceModuleDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "<resource type='velocity' name='input-params' location='bar' />" +
                "</animal>").getRootElement());

        final List resourceDescriptors = descriptor.getResourceDescriptors("velocity");
        assertNotNull(resourceDescriptors);
        assertEquals(2, resourceDescriptors.size());

        ResourceDescriptor resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(0);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()), resourceDescriptor);

        resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(1);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='input-params' location='bar' />").getRootElement()), resourceDescriptor);
    }

    private ResourcedModuleDescriptor makeResourceModuleDescriptor()
    {
        ResourcedModuleDescriptor descriptor = new ResourcedModuleDescriptor() {
            public Object getModule()
            {
                return null;
            }
        };
        return descriptor;
    }
}