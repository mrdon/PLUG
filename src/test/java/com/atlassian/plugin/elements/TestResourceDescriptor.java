package com.atlassian.plugin.elements;

import org.dom4j.DocumentHelper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import junit.framework.TestCase;

public class TestResourceDescriptor extends TestCase
{
    public void testBasicResource() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"/foo/bar.vm\" />");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertEquals("/foo/bar.vm", descriptor.getLocation());
        assertNull(descriptor.getContent());
    }

    public void testResourceWithContent() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">the content</resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertNull(descriptor.getLocation());
        assertEquals("the content", descriptor.getContent());
    }
}
