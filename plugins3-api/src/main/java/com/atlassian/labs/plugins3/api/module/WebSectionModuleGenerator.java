package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleName;
import org.dom4j.Element;

/**
 *
 */
@ModuleName("web-section")
public class WebSectionModuleGenerator extends AbstractWebFragmentModuleGenerator<WebSectionModuleGenerator>
{
    public WebSectionModuleGenerator(Element element)
    {
        super(element);
    }

    public WebSectionModuleGenerator location(String location)
    {
        element.addAttribute("location", location);
        return this;
    }
}
