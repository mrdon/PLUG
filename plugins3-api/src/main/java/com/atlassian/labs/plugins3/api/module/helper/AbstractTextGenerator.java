package com.atlassian.labs.plugins3.api.module.helper;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addParameterElement;

/**
 *
 */
public class AbstractTextGenerator<T extends AbstractTextGenerator>
{
    protected final Element element;

    public AbstractTextGenerator(String name)
    {
        DocumentFactory factory = DocumentFactory.getInstance();
        this.element = factory.createElement(name);
    }

    public Element getElement()
    {
        return element;
    }

    public T value(String value)
    {
        element.setText(value);
        return (T) this;
    }

    public T i18nKey(String key)
    {
        element.addAttribute("key", key);
        return (T) this;
    }

    public T addParameter(String key, String value)
    {
        addParameterElement(element, key, value);
        return (T) this;
    }
}
