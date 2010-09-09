package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleName;
import org.dom4j.Element;

import javax.servlet.http.HttpServlet;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addTextElement;


/**
 *
 */
@ModuleName("servlet")
public class ServletModuleGenerator extends AbstractClassModuleGenerator<ServletModuleGenerator, HttpServlet>
{

    public ServletModuleGenerator(Element element)
    {
        super(element);
    }

    public String getElementName()
    {
        return "servlet";
    }

    public ServletModuleGenerator addUrlPattern(String pattern)
    {
        addTextElement(element, "url-pattern", pattern);
        return this;
    }

    public ServletModuleGenerator addInitParam(String name, String value)
    {
        Element e = this.element.addElement("init-param");
        addTextElement(e, "param-name", name);
        addTextElement(e, "param-value", value);
        return this;
    }
}
