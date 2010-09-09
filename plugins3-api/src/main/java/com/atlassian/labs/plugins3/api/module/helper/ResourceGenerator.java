package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addParameterElement;

/**
 *
 */
public class ResourceGenerator
{
    private final Element element;
    
    public ResourceGenerator()
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement("resource");
    }

    public static ResourceGenerator resource()
    {
        return new ResourceGenerator();
    }
    
    public Element getElement()
    {
        return element;
    }
    
    public ResourceGenerator name(String name)
    {
        element.addAttribute("name", name);
        return this;
    }
    
    public ResourceGenerator namePattern(String namePattern)
    {
        element.addAttribute("namePattern", namePattern);
        return this;
    }
    
    public ResourceGenerator type(String type)
    {
        element.addAttribute("type", type);
        return this;
    }
    
    public ResourceGenerator location(String location)
    {
        element.addAttribute("location", location);
        return this;
    }

    public ResourceGenerator contentType(String contentType)
    {
        element.addAttribute("content-type", contentType);
        return this;
    }

    public ResourceGenerator content(String content)
    {
        element.setText(content);
        return this;
    }

    public ResourceGenerator addParameter(String key, String value)
    {
        addParameterElement(element, key, value);
        return this;
    }
}
