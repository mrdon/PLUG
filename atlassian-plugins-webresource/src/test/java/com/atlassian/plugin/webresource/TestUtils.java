package com.atlassian.plugin.webresource;

import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

public class TestUtils
{
    static List<ResourceDescriptor> createResourceDescriptors(String... resourceNames) throws DocumentException
    {
        List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        for(String resourceName : resourceNames)
        {
            resourceDescriptors.add(createResourceDescriptor(resourceName));
        }
        return resourceDescriptors;
    }

    static ResourceDescriptor createResourceDescriptor(String resourceName) throws DocumentException
    {
        return createResourceDescriptor(resourceName, new TreeMap<String, String>());
    }

    static ResourceDescriptor createResourceDescriptor(String resourceName, Map<String, String> parameters) throws DocumentException
    {
        String xml = "<resource type=\"download\" name=\"" + resourceName + "\" location=\"/includes/css/" + resourceName + "\">\n" +
                            "<param name=\"source\" value=\"webContext\"/>\n";

        if(resourceName.indexOf("ie") != -1)
            parameters.put("ieonly", "true");

        for(String key : parameters.keySet())
        {
            xml += "<param name=\"" + escapeXMLCharacters(key) + "\" value=\"" + escapeXMLCharacters(parameters.get(key)) + "\"/>\n";
        }
        
        xml += "</resource>";
        return new ResourceDescriptor(DocumentHelper.parseText(xml).getRootElement());
    }

    private static String escapeXMLCharacters(String input)
    {
        return input.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt");
    }
}
