package com.atlassian.plugin.elements;

import org.dom4j.Element;

public class ResourceDescriptor
{
    String type;
    String name;
    String location;
    private String content;

    public ResourceDescriptor(Element element)
    {
        this.type = element.attributeValue("type");
        this.name = element.attributeValue("name");
        this.location = element.attributeValue("location");

        if (element.getTextTrim() != null && !"".equals(element.getTextTrim()))
        {
            content = element.getTextTrim();
        }
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getLocation()
    {
        return location;
    }

    public String getContent()
    {
        return content;
    }
}
