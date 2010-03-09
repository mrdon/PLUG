package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.conditions.AbstractCompositeCondition;
import com.atlassian.plugin.web.conditions.AndCompositeCondition;
import com.atlassian.plugin.web.conditions.InvertedCondition;
import com.atlassian.plugin.web.conditions.OrCompositeCondition;
import com.atlassian.plugin.web.model.WebParam;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractWebFragmentModuleDescriptor<T> extends AbstractModuleDescriptor<T> implements WebFragmentModuleDescriptor<T>
{
    protected WebInterfaceManager webInterfaceManager;
    protected Element element;
    protected int weight;
    protected Condition condition;
    protected ContextProvider contextProvider;
    protected WebParam params;

    protected AbstractWebFragmentModuleDescriptor(ModuleFactory moduleClassFactory, final WebInterfaceManager webInterfaceManager)
    {
        super(moduleClassFactory);
        this.webInterfaceManager = webInterfaceManager;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        this.element = element;
        weight = 1000;
        try
        {
            weight = Integer.parseInt(element.attributeValue("weight"));
        }
        catch (final NumberFormatException e)
        {}
    }

    /**
     * Create a condition for when this web fragment should be displayed
     * @param element Element of web-section or web-item
     * @param type logical operator type {@link #getCompositeType}
     * @throws com.atlassian.plugin.PluginParseException
     */
    protected Condition makeConditions(final Element element, final int type) throws PluginParseException
    {
        //make single conditions (all Anded together)
        final List singleConditionElements = element.elements("condition");
        Condition singleConditions = null;
        if ((singleConditionElements != null) && !singleConditionElements.isEmpty())
        {
            singleConditions = makeConditions(singleConditionElements, type);
        }

        //make composite conditions (logical operator can be specified by "type")
        final List nestedConditionsElements = element.elements("conditions");
        AbstractCompositeCondition nestedConditions = null;
        if ((nestedConditionsElements != null) && !nestedConditionsElements.isEmpty())
        {
            nestedConditions = getCompositeCondition(type);
            for (final Iterator iterator = nestedConditionsElements.iterator(); iterator.hasNext();)
            {
                final Element nestedElement = (Element) iterator.next();
                nestedConditions.addCondition(makeConditions(nestedElement, getCompositeType(nestedElement.attributeValue("type"))));
            }
        }

        if ((singleConditions != null) && (nestedConditions != null))
        {
            //Join together the single and composite conditions by this type
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
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

    protected Condition makeConditions(final List elements, final int type) throws PluginParseException
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
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            for (final Iterator it = elements.iterator(); it.hasNext();)
            {
                final Element element = (Element) it.next();
                compositeCondition.addCondition(makeCondition(element));
            }

            return compositeCondition;
        }
    }

    protected Condition makeCondition(final Element element) throws PluginParseException
    {
        try
        {
            final Condition condition = webInterfaceManager.getWebFragmentHelper()
                .loadCondition(element.attributeValue("class"), plugin);
            condition.init(LoaderUtils.getParams(element));

            if ((element.attribute("invert") != null) && "true".equals(element.attributeValue("invert")))
            {
                return new InvertedCondition(condition);
            }

            return condition;
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured condition class does not implement the Condition interface");
        }
        catch (final Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    protected ContextProvider makeContextProvider(final Element element) throws PluginParseException
    {
        try
        {
            final ContextProvider context = webInterfaceManager.getWebFragmentHelper()
                .loadContextProvider(element.attributeValue("class"), plugin);
            context.init(LoaderUtils.getParams(element));

            return context;
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured context-provider class does not implement the ContextProvider interface");
        }
        catch (final Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    private int getCompositeType(final String type) throws PluginParseException
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

    private AbstractCompositeCondition getCompositeCondition(final int type) throws PluginParseException
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

    public int getWeight()
    {
        return weight;
    }

    public Condition getCondition()
    {
        return condition;
    }

    public ContextProvider getContextProvider()
    {
        return contextProvider;
    }

    public WebParam getWebParams()
    {
        return params;
    }

    @Override
    public void disabled()
    {
        webInterfaceManager.refresh();
        super.disabled();
    }

    public void setWebInterfaceManager(final WebInterfaceManager webInterfaceManager)
    {
        this.webInterfaceManager = webInterfaceManager;
    }
}
