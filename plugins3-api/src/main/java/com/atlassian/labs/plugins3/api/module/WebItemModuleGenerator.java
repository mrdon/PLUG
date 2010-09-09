package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleName;
import com.atlassian.labs.plugins3.api.module.helper.IconGenerator;
import com.atlassian.labs.plugins3.api.module.helper.LinkGenerator;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addTextElement;

/**
 *
 */
@ModuleName("web-item")
public class WebItemModuleGenerator extends AbstractWebFragmentModuleGenerator<WebItemModuleGenerator>
{
    public WebItemModuleGenerator(Element element)
    {
        super(element);
    }

    public String getElementName()
    {
        return "web-item";
    }

    public WebItemModuleGenerator section(String section)
    {
        element.addAttribute("section", section);
        return this;
    }

    public WebItemModuleGenerator styleClass(String styleClass)
    {
        addTextElement(element, "styleClass", styleClass);
        return this;
    }

    public WebItemModuleGenerator icon(IconGenerator iconGenerator)
    {
        element.add(iconGenerator.getElement());
        return this;
    }

    public WebItemModuleGenerator link(LinkGenerator linkGenerator)
    {
        element.add(linkGenerator.getElement());
        return this;
    }
}
