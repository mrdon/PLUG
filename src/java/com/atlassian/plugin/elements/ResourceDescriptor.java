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

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ResourceDescriptor)) return false;

        final ResourceDescriptor resourceDescriptor = (ResourceDescriptor) o;

        if (!name.equals(resourceDescriptor.name)) return false;
        if (!type.equals(resourceDescriptor.type)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = type.hashCode();
        result = 29 * result + name.hashCode();
        return result;
    }
}
