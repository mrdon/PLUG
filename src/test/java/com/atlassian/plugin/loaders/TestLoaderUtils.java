package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.util.List;
import java.util.Map;

public class TestLoaderUtils extends TestCase
{
    public void testMultipleResources() throws DocumentException, PluginParseException
    {
        Document document = DocumentHelper.parseText("<foo>" +
                "<resource type=\"velocity\" name=\"view\">the content</resource>" +
                "<resource type=\"velocity\" name=\"edit\" />" +
                "</foo>");

        List descriptors = LoaderUtils.getResourceDescriptors(document.getRootElement());
        assertEquals(2, descriptors.size());

        ResourceDescriptor first = (ResourceDescriptor) descriptors.get(0);
        assertEquals("velocity", first.getType());
        assertEquals("view", first.getName());
        ResourceDescriptor second = (ResourceDescriptor) descriptors.get(1);
        assertEquals("velocity", second.getType());
        assertEquals("edit", second.getName());
    }

    public void testMultipleResourceWithClashingKeysFail() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<foo>" +
                "<resource type=\"velocity\" name=\"view\">the content</resource>" +
                "<resource type=\"velocity\" name=\"view\" />" +
                "</foo>");

        try
        {
            LoaderUtils.getResourceDescriptors(document.getRootElement());
            fail("Should have thrown exception about duplicate resources.");
        }
        catch (Exception e)
        {
            assertEquals("Duplicate resource with type 'velocity' and name 'view' found", e.getMessage());
        }
    }


    public void testMultipleParameters() throws DocumentException
    {
        Document document = DocumentHelper.parseText("<foo>" +
                "<param name=\"colour\">green</param>" +
                "<param name=\"size\" value=\"large\" />" +
                "</foo>");

        Map params = LoaderUtils.getParams(document.getRootElement());
        assertEquals(2, params.size());

        assertEquals("green", params.get("colour"));
        assertEquals("large", params.get("size"));
    }

}