package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleGenerator;
import com.atlassian.labs.plugins3.api.module.helper.ResourceGenerator;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addParameterElement;
import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addTextElement;

/**
 *
 */
public abstract class AbstractModuleGenerator<T extends ModuleGenerator> implements ModuleGenerator<T>
{
    protected final Element element;

    public AbstractModuleGenerator(Element element)
    {
        this.element = element;
    }

    public T addResource(ResourceGenerator resourceGenerator)
    {
        element.add(resourceGenerator.getElement());
        return (T) this;
    }

    public T enabledByDefault(boolean enabledByDefault)
    {
        if (enabledByDefault)
        {
            element.addAttribute("state", "disabled");
        }
        else if (element.attribute("state") != null)
        {
            element.remove(element.attribute("state"));
        }
        return (T) this;
    }

    public T addParameter(String key, String value)
    {
        addParameterElement(element, key, value);
        return (T) this;
    }

    public T descriptionI18nKey(String descriptionKey)
    {
        Element desc = element.addElement("description");
        desc.addAttribute("key", descriptionKey);
        return (T) this;
    }

    public T description(String description)
    {
        addTextElement(element, "description", description);
        return (T) this;
    }

    public T nameI18nKey(String i18nNameKey)
    {
        element.addAttribute("i18n-name-key", i18nNameKey);
        return (T) this;
    }

    public T name(String name)
    {
        element.addAttribute("name", name);
        return (T) this;
    }
}
