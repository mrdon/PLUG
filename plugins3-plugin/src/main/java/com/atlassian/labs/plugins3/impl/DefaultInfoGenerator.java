package com.atlassian.labs.plugins3.impl;

import com.atlassian.labs.plugins3.api.InfoGenerator;
import org.dom4j.Document;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.createElement;

/**
 *
 */
public class DefaultInfoGenerator implements InfoGenerator<DefaultInfoGenerator>
{
    private final Element element;

    public DefaultInfoGenerator(Document document)
    {
        Element info = createElement(document.getRootElement(), "plugin-info");
        this.element = info;
    }



    public DefaultInfoGenerator name(String name)
    {
        element.addAttribute("name", name);
        return this;
    }

    public DefaultInfoGenerator description(String description)
    {
        Element desc = createElement(element, "description");
        desc.setText(description);
        return this;
    }

    public DefaultInfoGenerator descriptionI18nKey(String descriptionKey)
    {
        Element desc = createElement(element, "description");
        desc.addAttribute("key", descriptionKey);
        return this;
    }

    public DefaultInfoGenerator version(String version)
    {
        Element desc = createElement(element, "version");
        desc.setText(version);
        return this;
    }

    public DefaultInfoGenerator vendorName(String vendorName)
    {
        Element desc = createElement(element, "vendor");
        desc.addAttribute("name", vendorName);
        return this;
    }

    public DefaultInfoGenerator vendorUrl(String vendorUrl)
    {
        Element desc = createElement(element, "vendor");
        desc.addAttribute("url", vendorUrl);
        return this;
    }

    public DefaultInfoGenerator addParameter(String key, String value)
    {
        Element desc = element.addElement("param");
        desc.addAttribute("name", key);
        desc.setText(value);
        return this;
    }
}
