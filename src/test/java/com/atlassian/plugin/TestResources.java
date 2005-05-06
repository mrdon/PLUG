package com.atlassian.plugin;

import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import java.util.List;

import com.atlassian.plugin.elements.ResourceDescriptor;
import junit.framework.TestCase;

public class TestResources extends TestCase
{
    private static final String RESOURCE_DOC = "<foo>" +
                    "<resource type=\"velocity\" name=\"view\">the content</resource>" +
                    "<resource type=\"velocity\" name=\"edit\" />" +
                    "<resource type=\"image\" name=\"view\" />" +
                    "</foo>";

    public void testMultipleResources() throws DocumentException, PluginParseException
    {
        Resources resources = makeTestResources();

        List descriptors = resources.getResourceDescriptors();
        assertEquals(3, descriptors.size());

        assertDescriptorMatches((ResourceDescriptor) descriptors.get(0), "velocity", "view");
        assertDescriptorMatches((ResourceDescriptor) descriptors.get(1), "velocity", "edit");
        assertDescriptorMatches((ResourceDescriptor) descriptors.get(2), "image", "view");
    }

    public void testGetResourceDescriptorsByType() throws DocumentException, PluginParseException
    {
        Resources resources = makeTestResources();

        assertEquals(0, resources.getResourceDescriptors(null).size());
        assertEquals(0, resources.getResourceDescriptors("blah").size());

        List velocityResources = resources.getResourceDescriptors("velocity");
        assertEquals(2, velocityResources.size());

        assertDescriptorMatches((ResourceDescriptor) velocityResources.get(0), "velocity", "view");
        assertDescriptorMatches((ResourceDescriptor) velocityResources.get(1), "velocity", "edit");
    }

    public void testGetResourceDescriptor() throws DocumentException, PluginParseException
    {
        Resources resources = makeTestResources();

        assertNull(resources.getResourceDescriptor("image", "edit"));
        assertNull(resources.getResourceDescriptor("fish", "view"));
        assertNull(resources.getResourceDescriptor(null, "view"));
        assertNull(resources.getResourceDescriptor("image", null));

        assertDescriptorMatches(resources.getResourceDescriptor("image", "view"), "image", "view");

    }

    public void testMultipleResourceWithClashingKeysFail() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<foo>" +
                "<resource type=\"velocity\" name=\"view\">the content</resource>" +
                "<resource type=\"velocity\" name=\"view\" />" +
                "</foo>");

        try
        {

            Resources.fromXml(document.getRootElement());
            fail("Should have thrown exception about duplicate resources.");
        }
        catch (Exception e)
        {
            assertEquals("Duplicate resource with type 'velocity' and name 'view' found", e.getMessage());
        }
    }

    private void assertDescriptorMatches(ResourceDescriptor first, String type, String name)
    {
        assertEquals(type, first.getType());
        assertEquals(name, first.getName());
    }

    private Resources makeTestResources()
            throws DocumentException, PluginParseException
    {
        Document document = DocumentHelper.parseText(RESOURCE_DOC);

        Resources resources = Resources.fromXml(document.getRootElement());
        return resources;
    }
}
