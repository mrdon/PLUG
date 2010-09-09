package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

/**
 *
 */
public class IconGenerator
{
    private final Element element;

    public IconGenerator()
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement("icon");
    }

    public static IconGenerator icon()
    {
        return new IconGenerator();
    }

    public Element getElement()
    {
        return element;
    }

    public IconGenerator link(LinkGenerator linkGenerator)
    {
        element.add(linkGenerator.getElement());
        return this;
    }
    public IconGenerator width(int width)
    {
        element.addAttribute("width", String.valueOf(width));
        return this;
    }

    public IconGenerator height(int height)
    {
        element.addAttribute("height", String.valueOf(height));
        return this;
    }
}
