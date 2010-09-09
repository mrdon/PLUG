package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

/**
 *
 */
public class ConditionsGenerator
{
    public static enum Type
    {
        AND,
        OR
    }
    private final Element element;

    public ConditionsGenerator()
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement("conditions");
    }

    public static ConditionsGenerator or(ConditionsGenerator... conditionsGenerators)
    {
        return addConditionsGenerators(conditionsGenerators, "or");
    }

    public static ConditionsGenerator and(ConditionsGenerator... conditionsGenerators)
    {
        return addConditionsGenerators(conditionsGenerators, "and");
    }

    private static ConditionsGenerator addConditionsGenerators(ConditionsGenerator[] conditionsGenerators, String type)
    {
        ConditionsGenerator generator = new ConditionsGenerator();
        generator.element.addAttribute("type", type);
        for (ConditionsGenerator gen : conditionsGenerators)
        {
            generator.element.add(gen.getElement());
        }
        return generator;
    }

    public static  ConditionsGenerator or(ConditionGenerator... conditionGenerators)
    {
        return addConditionGenerators(conditionGenerators, "or");
    }

    public static ConditionsGenerator and(ConditionGenerator... conditionGenerators)
    {
        return addConditionGenerators(conditionGenerators, "and");
    }

    private static ConditionsGenerator addConditionGenerators(ConditionGenerator[] conditionGenerators, String type)
    {
        ConditionsGenerator generator = new ConditionsGenerator();
        generator.element.addAttribute("type", type);
        for (ConditionGenerator gen : conditionGenerators)
        {
            generator.element.add(gen.getElement());
        }
        return generator;
    }

    public Element getElement()
    {
        return element;
    }
}
