package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.web.conditions.InvertedCondition;
import com.atlassian.plugin.web.conditions.AndCompositeCondition;
import com.atlassian.plugin.web.conditions.OrCompositeCondition;
import com.atlassian.plugin.web.conditions.AbstractCompositeCondition;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
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
    public static final int COMPOSITE_TYPE_OR = 0;
    public static final int COMPOSITE_TYPE_AND = 1;

    protected WebInterfaceManager webInterfaceManager;
    protected int weight;
    protected Condition condition;
    protected ContextProvider contextProvider;
    protected WebLabel label;
    protected WebLabel tooltip;

    protected AbstractWebFragmentModuleDescriptor(WebInterfaceManager webInterfaceManager)
    {
        this.webInterfaceManager = webInterfaceManager;
    }

    public AbstractWebFragmentModuleDescriptor()
    {
    }

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
        if (element.element("context-provider") != null)
        {
            contextProvider = makeContextProvider(element.element("context-provider"));
        }
        if (element.element("label") != null)
            label = new WebLabel(element.element("label"), webInterfaceManager.getWebFragmentHelper(), contextProvider);
        condition = makeConditions(element, COMPOSITE_TYPE_AND);
        if (element.element("tooltip") != null)
            tooltip = new WebLabel(element.element("tooltip"), webInterfaceManager.getWebFragmentHelper(), contextProvider);
    }

    /**
     * Create a condition for when this web fragment should be displayed
     * @param element Element of web-section or web-item
     * @param type logical operator type {@link #getCompositeType}
     * @throws PluginParseException
     */
    protected Condition makeConditions(Element element, int type) throws PluginParseException
    {
        //make single conditions (all Anded together)
        List singleConditionElements = element.elements("condition");
        Condition singleConditions = null;
        if (singleConditionElements != null && !singleConditionElements.isEmpty())
        {
            singleConditions = makeConditions(singleConditionElements, type);
        }

        //make composite conditions (logical operator can be specified by "type")
        List nestedConditionsElements = element.elements("conditions");
        AbstractCompositeCondition nestedConditions = null;
        if (nestedConditionsElements != null && !nestedConditionsElements.isEmpty())
        {
            nestedConditions = getCompositeCondition(type);
            for (Iterator iterator = nestedConditionsElements.iterator(); iterator.hasNext();)
            {
                Element nestedElement = (Element) iterator.next();
                nestedConditions.addCondition(makeConditions(nestedElement, getCompositeType(nestedElement.attributeValue("type"))));
            }
        }

        if (singleConditions != null && nestedConditions != null)
        {
            //Join together the single and composite conditions by this type
            AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            compositeCondition.addCondition(singleConditions);
            compositeCondition.addCondition(nestedConditions);
            return compositeCondition;
        }
        else if (singleConditions != null)
        {
            return singleConditions;
        }
        else if (nestedConditions != null)
        {
            return nestedConditions;
        }

        return null;
    }

    protected Condition makeConditions(List elements, int type) throws PluginParseException
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
            AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
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

    protected ContextProvider makeContextProvider(Element element) throws PluginParseException
    {
        try
        {
            ContextProvider context = webInterfaceManager.getWebFragmentHelper().loadContextProvider(element.attributeValue("class"), plugin);
            context.init(LoaderUtils.getParams(element));

            return context;
        }
        catch (ClassCastException e)
        {
            throw new PluginParseException("Configured context-provider class does not implement the ContextProvider interface");
        }
        catch (Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    private int getCompositeType(String type) throws PluginParseException
    {
        if ("or".equalsIgnoreCase(type))
        {
            return COMPOSITE_TYPE_OR;
        }
        else if ("and".equalsIgnoreCase(type))
        {
            return COMPOSITE_TYPE_AND;
        }
        throw new PluginParseException("Invalid condition type specified. type = " + type);
    }

    private AbstractCompositeCondition getCompositeCondition(int type) throws PluginParseException
    {
        switch (type)
        {
            case COMPOSITE_TYPE_OR:
            {
                return new OrCompositeCondition();
            }
            case COMPOSITE_TYPE_AND:
            {
                return new AndCompositeCondition();
            }
        }
        throw new PluginParseException("Invalid condition type specified. type = " + type);
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

    public WebLabel getTooltip()
    {
        return tooltip;
    }

    public void setWebInterfaceManager(WebInterfaceManager webInterfaceManager)
    {
        this.webInterfaceManager = webInterfaceManager;
    }

    public Condition getCondition()
    {
        return condition;
    }

    public ContextProvider getContextProvider()
    {
        return contextProvider;
    }
}
