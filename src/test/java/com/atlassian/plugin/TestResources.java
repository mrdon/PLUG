package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.util.List;

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

        assertNull(resources.getResourceLocation("image", "edit"));
        assertNull(resources.getResourceLocation("fish", "view"));
        assertNull(resources.getResourceLocation(null, "view"));
        assertNull(resources.getResourceLocation("image", null));

        assertLocationMatches(resources.getResourceLocation("image", "view"), "image", "view");

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
        catch (PluginParseException e)
        {
            assertEquals("Duplicate resource with type 'velocity' and name 'view' found", e.getMessage());
        }
    }

    public void testParsingNullElementThrowsException() throws Exception
    {
        try
        {
            Resources.fromXml(null);
            fail("Expected exception when parsing null element");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

    public void testEmptyResources() throws Exception
    {
        Resources resources = Resources.EMPTY_RESOURCES;
        assertTrue("Empty resources should be empty", resources.getResourceDescriptors().isEmpty());
        assertTrue("Empty resources should be empty by type", resources.getResourceDescriptors("i18n").isEmpty());
        assertNull("Empty resources should return null for any resource", resources.getResourceLocation("i18n", "i18n.properties"));
    }

    private void assertLocationMatches(ResourceLocation first, String type, String name)
    {
        assertEquals(type, first.getType());
        assertEquals(name, first.getName());
    }

    private void assertDescriptorMatches(ResourceDescriptor first, String type, String name)
    {
        assertEquals(type, first.getType());
        assertEquals(name, first.getName());
    }

    private Resources makeTestResources() throws DocumentException, PluginParseException
    {
        Document document = DocumentHelper.parseText(RESOURCE_DOC);
        return Resources.fromXml(document.getRootElement());
    }
}
