package com.atlassian.plugin.webresource;

import org.dom4j.DocumentHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 *
 */
public class Dom4jFluent
{
    public static Map<String,String> attributes(String... attrs)
    {
        Map<String, String> result = new HashMap<String, String>();
        for (int x=0; x<attrs.length; x+= 2)
        {
            result.put(attrs[x], attrs[x+1]);
        }
        return result;
    }
    /**
     * Constructs the element with a textual body
     *
     * @param name  The element name
     * @param value The element text value
     * @return The element object
     */
    public static Element element(String name, String value)
    {
        return new Element(name, null, value, new Element[0]);
    }

    /**
     * Constructs the element containg child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     */
    public static Element element(String name, Element... elements)
    {
        return new Element(name, elements);
    }

    /**
     * Constructs the element containg child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     */
    public static Element element(String name, Iterable<Iterable<Element>> elements)
    {
        return new Element(name, elements);
    }

    /**
     * Constructs the element containg child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     */
    public static Element element(String name, Map<String, String> attributes, Element... elements)
    {
        return new Element(name, attributes, elements);
    }

    /**
     * Constructs the element containg child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     */
    public static Element element(String name, Map<String, String> attributes, Iterable<Iterable<Element>> elements)
    {
        return new Element(name, attributes, elements);
    }

    /**
     * Element wrapper class for configuration elements
     */
    public static class Element
    {
        private final Iterable<Iterable<Element>> children;
        private final String name;
        private final Map<String, String> attributes;
        private final String text;

        public Element(String name, Element... children)
        {
            this(name, null, children);
        }

        public Element(String name, Iterable<Iterable<Element>> children)
        {
            this(name, null, children);
        }

        public Element(String name, Map<String, String> attributes, Element... children)
        {
            this(name, attributes, null, children);
        }

        public Element(String name, Map<String, String> attributes, Iterable<Iterable<Element>> children)
        {
            this(name, attributes, null, children);
        }

        public Element(String name, Map<String, String> attributes, String text, Element... children)
        {
            this(name, attributes, text, Arrays.<Iterable<Element>>asList(asList(children)));
        }

        public Element(String name, Map<String, String> attributes, String text, Iterable<Iterable<Element>> children)
        {
            this.name = name;
            this.attributes = attributes;
            this.text = text;

            this.children = children;
        }

        public org.dom4j.Element toDom()
        {

            org.dom4j.Element dom = DocumentHelper.createElement(name);
            if (attributes != null)
            {
                for (Map.Entry<String,String> entry : attributes.entrySet())
                {
                    dom.addAttribute(entry.getKey(), entry.getValue());
                }
            }
            if (text != null)
            {
                dom.setText(text);
            }
            if (children != null)
            {
                for (Iterable<Element> i : children)
                {
                    for (Element e : i)
                    {
                        dom.add(e.toDom());
                    }
                }
            }

            return dom;
        }
    }

}
