package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleName;
import org.dom4j.Element;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addTextElement;

/**
 *
 */
@ModuleName("servlet-context-param")
public class ServletContextParamModuleGenerator extends AbstractModuleGenerator<ServletContextParamModuleGenerator>
{
    public ServletContextParamModuleGenerator(Element element)
    {
        super(element);
    }

    public String getElementName()
    {
        return "servlet-context-param";
    }

    public ServletContextParamModuleGenerator name(String name)
    {
        addTextElement(element, "param-name", name);
        return this;
    }

    public ServletContextParamModuleGenerator value(String name)
    {
        addTextElement(element, "param-value", name);
        return this;
    }
}
