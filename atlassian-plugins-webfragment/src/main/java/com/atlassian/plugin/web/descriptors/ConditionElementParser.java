package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.conditions.AbstractCompositeCondition;
import com.atlassian.plugin.web.conditions.AndCompositeCondition;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import com.atlassian.plugin.web.conditions.InvertedCondition;
import com.atlassian.plugin.web.conditions.OrCompositeCondition;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;

/**
 * This class contains the logic for constructing {@link com.atlassian.plugin.web.Condition}
 * objects from a module descriptor's XML element. Its functionality is used
 * by both {@link com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor}
 * and {@link com.atlassian.plugin.web.descriptors.DefaultWebPanelModuleDescriptor}.
 *
 * @since   2.5.0
 */
class ConditionElementParser
{
    public static final int COMPOSITE_TYPE_OR = WebFragmentModuleDescriptor.COMPOSITE_TYPE_OR;
    public static final int COMPOSITE_TYPE_AND = WebFragmentModuleDescriptor.COMPOSITE_TYPE_AND;

    private final WebFragmentHelper webFragmentHelper;

    public ConditionElementParser(WebFragmentHelper webFragmentHelper)
    {
        this.webFragmentHelper = webFragmentHelper;
    }

    /**
     * Create a condition for when this web fragment should be displayed.
     *
     * @param element Element of web-section, web-item, or web-panel.
     * @param type    logical operator type {@link #getCompositeType}
     * @throws com.atlassian.plugin.PluginParseException
     *
     */
    @SuppressWarnings("unchecked")
    public Condition makeConditions(final Plugin plugin, final Element element, final int type) throws PluginParseException
    {
        // make single conditions (all Anded together)
        final List<Element> singleConditionElements = element.elements("condition");
        Condition singleConditions = null;
        if ((singleConditionElements != null) && !singleConditionElements.isEmpty())
        {
            singleConditions = makeConditions(plugin, singleConditionElements, type);
        }

        // make composite conditions (logical operator can be specified by
        // "type")
        final List<Element> nestedConditionsElements = element.elements("conditions");
        AbstractCompositeCondition nestedConditions = null;
        if ((nestedConditionsElements != null) && !nestedConditionsElements.isEmpty())
        {
            nestedConditions = getCompositeCondition(type);
            for (final Iterator<Element> iterator = nestedConditionsElements.iterator(); iterator.hasNext();)
            {
                final Element nestedElement = iterator.next();
                nestedConditions.addCondition(makeConditions(plugin, nestedElement, getCompositeType(nestedElement.attributeValue("type"))));
            }
        }

        if ((singleConditions != null) && (nestedConditions != null))
        {
            // Join together the single and composite conditions by this type
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

    @SuppressWarnings("unchecked")
    public Condition makeConditions(final Plugin plugin, final List<Element> elements, final int type) throws PluginParseException
    {
        if (elements.isEmpty())
        {
            return null;
        }
        else if (elements.size() == 1)
        {
            return makeCondition(plugin, elements.get(0));
        }
        else
        {
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            for (final Iterator<Element> it = elements.iterator(); it.hasNext();)
            {
                final Element element = it.next();
                compositeCondition.addCondition(makeCondition(plugin, element));
            }

            return compositeCondition;
        }
    }

    public Condition makeCondition(final Plugin plugin, final Element element) throws PluginParseException
    {
        try
        {
            final Condition condition = webFragmentHelper.loadCondition(element.attributeValue("class"), plugin);
            condition.init(LoaderUtils.getParams(element));

            if ((element.attribute("invert") != null) && "true".equals(element.attributeValue("invert")))
            {
                return new InvertedCondition(condition);
            }

            return condition;
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured condition class does not implement the Condition interface", e);
        }
        catch (final ConditionLoadingException cle)
        {
            throw new PluginParseException("Unable to load the module's display conditions: " + cle.getMessage(), cle);
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
        else
        {
            throw new PluginParseException("Invalid condition type specified. type = " + type);
        }
    }

    private AbstractCompositeCondition getCompositeCondition(final int type) throws PluginParseException
    {
        switch (type)
        {
            case COMPOSITE_TYPE_OR:
                return new OrCompositeCondition();
            case COMPOSITE_TYPE_AND:
                return new AndCompositeCondition();
            default:
                throw new PluginParseException("Invalid condition type specified. type = " + type);
        }
    }
}