package com.atlassian.plugin.loaders;

import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;

import java.util.List;
import java.util.Map;

public class TestLoaderUtils extends TestCase
{
    public void testMultipleResources() throws DocumentException
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