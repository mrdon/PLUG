package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.util.concurrent.locks.Condition;

/**
 *
 */
public class ConditionGenerator
{
    private final Element element;

    public ConditionGenerator()
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement("conditions");
    }

    public static <T extends Condition> ConditionGenerator condition(Class<T> condition, boolean invert)
    {
        ConditionGenerator generator = new ConditionGenerator();
        generator.element.addAttribute("class", condition.getName());
        generator.element.addAttribute("invert", String.valueOf(invert));
        return generator;
    }

    public Element getElement()
    {
        return element;
    }
}
