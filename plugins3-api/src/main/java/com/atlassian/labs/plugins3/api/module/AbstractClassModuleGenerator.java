package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.ModuleGenerator;
import org.dom4j.Element;

/**
 *
 */
public abstract class AbstractClassModuleGenerator<T extends ModuleGenerator, M> extends AbstractModuleGenerator<T>
{
    public AbstractClassModuleGenerator(Element element)
    {
        super(element);
    }

    public T moduleClass(Class<? extends M> moduleClass)
    {
        element.addAttribute("class", moduleClass.getName());
        return (T) this;
    }
}
