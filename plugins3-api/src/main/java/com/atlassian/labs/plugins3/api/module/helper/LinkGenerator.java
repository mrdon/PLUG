package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.net.URI;

/**
 *
 */
public class LinkGenerator
{
    private final Element element;

    public LinkGenerator()
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement("link");
    }

    public static LinkGenerator link()
    {
        return new LinkGenerator();
    }

    public Element getElement()
    {
        return element;
    }

    public LinkGenerator id(String id)
    {
        element.addAttribute("linkId", id);
        return this;
    }

    public LinkGenerator accessKey(String accessKey)
    {
        element.addAttribute("accessKey", accessKey);
        return this;
    }

    public LinkGenerator absolute(boolean absolute)
    {
        if (absolute)
        {
            // case sensitive
            element.addAttribute("absolute", "true");
        }

        return this;
    }

    public LinkGenerator uri(URI uri)
    {
        element.setText(uri.toString());
        return this;
    }
}
