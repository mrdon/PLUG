/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:53:39 PM
 */
package com.atlassian.plugin.descriptors;

import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.mock.MockResourceParameterGenerator;

public class TestResourcedModuleDescriptor extends TestCase
{
    public void testNoResourceGenerator() throws DocumentException, PluginParseException
    {
        ResourcedModuleDescriptor descriptor = makeResourceModuleDescriptor();
        descriptor.init(null, DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">the content</animal>").getRootElement());
        assertFalse(descriptor.hasParameterGenerator());
    }

    public void testHasResourceGenerator() throws DocumentException, PluginParseException
    {
        ResourcedModuleDescriptor descriptor = makeResourceModuleDescriptor();
        descriptor.init(null, DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<param name=\"resource.parameter.generator\">com.atlassian.plugin.mock.MockResourceParameterGenerator</param>" +
                "</animal>").getRootElement());
        assertTrue(descriptor.hasParameterGenerator());
        assertEquals(new MockResourceParameterGenerator(), descriptor.getParameterGenerator());
    }

    public void testGetResourceDescriptor() throws DocumentException, PluginParseException
    {
        ResourcedModuleDescriptor descriptor = makeResourceModuleDescriptor();
        descriptor.init(null, DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "</animal>").getRootElement());

        assertNull(descriptor.getResourceDescriptor("foo", "bar"));
        assertNull(descriptor.getResourceDescriptor("velocity", "bar"));
        assertNull(descriptor.getResourceDescriptor("foo", "view"));
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()), descriptor.getResourceDescriptor("velocity", "view"));
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