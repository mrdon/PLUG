package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleName;
import com.atlassian.plugin.servlet.filter.FilterDispatcherCondition;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import org.dom4j.Element;

import javax.servlet.Filter;
import java.util.Locale;

import static com.atlassian.labs.plugins3.api.module.ModuleBuilderUtil.addTextElement;

/**
 *
 */
@ModuleName("servlet-filter")
public class ServletFilterModuleGenerator extends AbstractClassModuleGenerator<ServletFilterModuleGenerator, Filter>
{
    public ServletFilterModuleGenerator(Element element)
    {
        super(element);
    }

    public ServletFilterModuleGenerator location(FilterLocation location)
    {
        element.addAttribute("location", location.name().toLowerCase(Locale.ENGLISH).replace('_','-'));
        return this;
    }

    public ServletFilterModuleGenerator weight(int weight)
    {
        element.addAttribute("weight", String.valueOf(weight));
        return this;
    }

    public ServletFilterModuleGenerator addDispatcher(FilterDispatcherCondition dispatcher)
    {
        addTextElement(element, "dispatcher", dispatcher.name());
        return this;
    }

    public ServletFilterModuleGenerator addUrlPattern(String pattern)
    {
        addTextElement(element, "url-pattern", pattern);
        return this;
    }

    public ServletFilterModuleGenerator addInitParam(String name, String value)
    {
        Element e = this.element.addElement("init-param");
        addTextElement(e, "param-name", name);
        addTextElement(e, "param-value", value);
        return this;
    }

    public String getElementName()
    {
        return "servlet-filter";
    }
}
