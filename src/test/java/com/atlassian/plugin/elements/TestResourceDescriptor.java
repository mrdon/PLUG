package com.atlassian.plugin.elements;

import org.dom4j.DocumentHelper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import junit.framework.TestCase;
import com.atlassian.plugin.mock.MockBear;

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

    public void testResourceWithParameters() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"/foo/bar.vm\">" +
                "<param name=\"attribute\" value=\"20\"/>" +
                "<param name=\"content\">fish</param></resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertEquals("/foo/bar.vm", descriptor.getLocation());
        assertNull(descriptor.getContent());
        assertEquals("20", descriptor.getParameter("attribute"));
        assertEquals("fish", descriptor.getParameter("content"));
    }

    public void testResourceWithParametersAndContent() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">" +
                "<param name=\"attribute\" value=\"20\"/>" +
                "<param name=\"content\">fish</param>" +
                "This is the content.</resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertNull(descriptor.getLocation());
        assertEquals("This is the content.", descriptor.getContent());
        assertEquals("20", descriptor.getParameter("attribute"));
        assertEquals("fish", descriptor.getParameter("content"));
    }

    public void testEquality() throws DocumentException
    {
        Document velViewDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">the content</resource>");
        Document velViewDoc2= DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"foo\" />");
        Document velViewDoc3= DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"foo\"><param name=\"narrator\">Tyler Durden</param></resource>");
        Document velEditDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"edit\">the content</resource>");
        Document fooEditDoc = DocumentHelper.parseText("<resource type=\"foo\" name=\"edit\">the content</resource>");
        ResourceDescriptor velViewResource = new ResourceDescriptor(velViewDoc.getRootElement());
        ResourceDescriptor velViewResource2 = new ResourceDescriptor(velViewDoc2.getRootElement());
        ResourceDescriptor velViewResource3 = new ResourceDescriptor(velViewDoc3.getRootElement());
        ResourceDescriptor velEditResource = new ResourceDescriptor(velEditDoc.getRootElement());
        ResourceDescriptor fooEditResource = new ResourceDescriptor(fooEditDoc.getRootElement());

        assertFalse(velEditResource.equals(new MockBear()));
        assertEquals(velViewResource, velViewResource);
        assertEquals(velViewResource, velViewResource2);
        assertEquals(velViewResource, velViewResource3);
        assertEquals(velViewResource2, velViewResource);
        assertEquals(velViewResource3, velViewResource);
        assertFalse(velViewResource.equals(velEditResource));
        assertFalse(velEditResource.equals(velViewResource));
        assertFalse(fooEditResource.equals(velEditResource));
    }
}
