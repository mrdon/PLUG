package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.web.conditions.CompositeCondition;
import com.atlassian.plugin.web.conditions.InvertedCondition;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.model.WebLabel;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.List;
import java.util.Iterator;

/**
 * An abstract class providing utility methods to different forms of web fragment descriptors.
 */
public abstract class AbstractWebFragmentModuleDescriptor extends AbstractModuleDescriptor implements StateAware, WeightedDescriptor
{
    protected WebInterfaceManager webInterfaceManager;
    protected int weight;
    protected Condition condition;
    protected WebLabel label;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        weight = 1000;
        try
        {
            weight = Integer.parseInt(element.attributeValue("weight"));
        }
        catch (NumberFormatException e)
        {
        }
        label = new WebLabel(element.element("label"), webInterfaceManager.getWebFragmentHelper());
        condition = makeConditions(element.elements("condition"));
    }

    protected Condition makeConditions(List elements) throws PluginParseException
    {
        if (elements.size() == 0)
        {
            return null;
        }
        else if (elements.size() == 1)
        {
            return makeCondition((Element) elements.get(0));
        }
        else
        {
            CompositeCondition compositeCondition = new CompositeCondition();
            for (Iterator it = elements.iterator(); it.hasNext();)
            {
                Element element = (Element) it.next();
                compositeCondition.addCondition(makeCondition(element));
            }

            return compositeCondition;
        }
    }

    protected Condition makeCondition(Element element) throws PluginParseException
    {
        try
        {
            Condition condition = webInterfaceManager.getWebFragmentHelper().loadCondition(element.attributeValue("class"), plugin);
            condition.init(LoaderUtils.getParams(element));

            if (element.attribute("invert") != null && "true".equals(element.attributeValue("invert")))
            {
                return new InvertedCondition(condition);
            }

            return condition;
        }
        catch (ClassCastException e)
        {
            throw new PluginParseException("Configured condition class does not implement the Condition interface");
        }
        catch (Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    public void enabled()
    {
        webInterfaceManager.refresh();
    }

    public void disabled()
    {
        webInterfaceManager.refresh();
    }

    public int getWeight()
    {
        return weight;
    }

    public Object getModule()
    {
        return null;
    }

    public WebLabel getWebLabel()
    {
        return label;
    }

    public void setWebInterfaceManager(WebInterfaceManager webInterfaceManager)
    {
        this.webInterfaceManager = webInterfaceManager;
    }

    public Condition getCondition()
    {
        return condition;
    }
}
