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

    public void testEquality() throws DocumentException
    {
        Document velViewDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">the content</resource>");
        Document velViewDoc2= DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"foo\" />");
        Document velEditDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"edit\">the content</resource>");
        ResourceDescriptor velViewResource = new ResourceDescriptor(velViewDoc.getRootElement());
        ResourceDescriptor velViewResource2 = new ResourceDescriptor(velViewDoc2.getRootElement());
        ResourceDescriptor velEditResource = new ResourceDescriptor(velEditDoc.getRootElement());

        assertEquals(velViewResource, velViewResource2);
        assertEquals(velViewResource2, velViewResource);
        assertFalse(velViewResource.equals(velEditResource));
        assertFalse(velEditResource.equals(velViewResource));
    }
}
