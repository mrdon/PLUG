package com.atlassian.labs.plugins3.api.module;

import org.dom4j.Element;

/**
 *
 */
public class ModuleBuilderUtil
{
    public static Element createElement(Element rootElement, String name)
    {
        Element e = rootElement.element(name);
        if (e == null)
        {
            e = rootElement.addElement(name);
        }
        return e;
    }

    public static Element addTextElement(Element rootElement, String name, String value)
    {
        Element e = rootElement.addElement(name);
        e.setText(value);
        return e;
    }

    public static void addParameterElement(Element rootElement, String key, String value)
    {
        Element desc = rootElement.addElement("param");
        desc.addAttribute("name", key);
        desc.setText(value);
    }
}
