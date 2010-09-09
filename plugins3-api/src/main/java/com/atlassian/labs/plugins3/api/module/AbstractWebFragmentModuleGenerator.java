package com.atlassian.labs.plugins3.api.module;

import com.atlassian.labs.plugins3.api.module.helper.ConditionGenerator;
import com.atlassian.labs.plugins3.api.module.helper.ConditionsGenerator;
import com.atlassian.labs.plugins3.api.module.helper.LabelGenerator;
import com.atlassian.labs.plugins3.api.module.helper.TooltipGenerator;
import org.dom4j.Element;

/**
 *
 */
public abstract class AbstractWebFragmentModuleGenerator<T extends AbstractWebFragmentModuleGenerator> extends AbstractModuleGenerator
{

    public AbstractWebFragmentModuleGenerator(Element element)
    {
        super(element);
    }

    public T weight(int weight)
    {
        element.addAttribute("weight", String.valueOf(weight));
        return (T) this;
    }

    public T label(LabelGenerator labelGenerator)
    {
        element.add(labelGenerator.getElement());
        return (T) this;
    }

    public T tooltip(TooltipGenerator tooltipGenerator)
    {
        element.add(tooltipGenerator.getElement());
        return (T) this;
    }

    public T conditions(ConditionsGenerator... conditionsGenerators)
    {
        ConditionsGenerator generator = new ConditionsGenerator();
        for (ConditionsGenerator gen : conditionsGenerators)
        {
            element.add(gen.getElement());
        }
        return (T) this;
    }

    public T conditions(ConditionGenerator... conditionGenerators)
    {
        ConditionsGenerator generator = new ConditionsGenerator();
        for (ConditionGenerator gen : conditionGenerators)
        {
            element.add(gen.getElement());
        }
        return (T) this;
    }
}
