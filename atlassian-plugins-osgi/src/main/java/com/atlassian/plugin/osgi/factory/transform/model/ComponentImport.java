package com.atlassian.plugin.osgi.factory.transform.model;

import org.dom4j.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the data in a component-import tag in the plugin descriptor
 *
 * @since 2.2.0
 */
public class ComponentImport
{
    private final String key;
    private final Set<String> interfaces;

    public ComponentImport(Element element)
    {
        this.key = element.attributeValue("key");
        this.interfaces = new HashSet<String>();
        if (element.attribute("interface") != null)
        {
            interfaces.add(element.attributeValue("interface"));
        }
        else
        {
            List<Element> compInterfaces = element.elements("interface");
            for (Element inf : compInterfaces)
            {
                interfaces.add(inf.getTextTrim());
            }
        }
    }
    public ComponentImport(String key, Set<String> interfaces)
    {
        this.key = key;
        this.interfaces = interfaces;
    }

    public String getKey()
    {
        return key;
    }

    public Set<String> getInterfaces()
    {
        return interfaces;
    }
}
