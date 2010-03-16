package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.model.DefaultWebLabel;
import com.atlassian.plugin.web.model.DefaultWebParam;
import com.atlassian.plugin.web.model.WebLabel;
import com.atlassian.plugin.web.model.WebParam;
import org.dom4j.Element;

import java.util.List;

/**
 * An abstract convenience class for web fragment descriptors.
 */
public abstract class AbstractWebFragmentModuleDescriptor<T> extends AbstractModuleDescriptor<T> implements StateAware, WebFragmentModuleDescriptor<T>
{
    protected WebInterfaceManager webInterfaceManager;
    protected Element element;
    protected int weight;

    protected Condition condition;
    protected ModuleDescriptorHelper moduleDescriptorHelper = null;
    protected ContextProvider contextProvider;
    protected DefaultWebLabel label;
    protected DefaultWebLabel tooltip;
    protected WebParam params;

    protected AbstractWebFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager)
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
        this.webInterfaceManager = webInterfaceManager;
        this.moduleDescriptorHelper = new ModuleDescriptorHelper(plugin, webInterfaceManager.getWebFragmentHelper());
    }

    public AbstractWebFragmentModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
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
     * @param type logical operator type {@link ModuleDescriptorHelper#getCompositeType(String)}
     * @throws PluginParseException
     */
    protected Condition makeConditions(final Element element, final int type) throws PluginParseException
    {
        return getRequiredModuleDescriptorHelper().makeConditions(element, type);
    }

    protected Condition makeConditions(final List elements, final int type) throws PluginParseException
    {
        return getRequiredModuleDescriptorHelper().makeConditions(elements, type);
    }

    protected Condition makeCondition(final Element element) throws PluginParseException
    {
        return getRequiredModuleDescriptorHelper().makeCondition(element);
    }

    protected ContextProvider makeContextProvider(final Element element) throws PluginParseException
    {
        return getRequiredModuleDescriptorHelper().makeContextProvider(element);
    }

    private ModuleDescriptorHelper getRequiredModuleDescriptorHelper()
    {
        if (moduleDescriptorHelper == null)
        {
            throw new IllegalStateException("ModuleDescriptorHelper not " +
                    "available because the WebInterfaceManager has not been injected.");
        }
        else
        {
            return moduleDescriptorHelper;
        }
    }

    @Override
    public void enabled()
    {
        super.enabled();
        // this was moved to the enabled() method because spring beans declared
        // by the plugin are not available for injection during the init() phase
        try
        {
            if (element.element("context-provider") != null)
            {
                contextProvider = makeContextProvider(element.element("context-provider"));
            }

            if (element.element("label") != null)
            {
                label = new DefaultWebLabel(element.element("label"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            if (element.element("tooltip") != null)
            {
                tooltip = new DefaultWebLabel(element.element("tooltip"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            if (getParams() != null)
            {
                params = new DefaultWebParam(getParams(), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            condition = makeConditions(element, ModuleDescriptorHelper.COMPOSITE_TYPE_AND);
        }
        catch (final PluginParseException e)
        {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web fragment", e);
        }

        webInterfaceManager.refresh();
    }

    @Override
    public void disabled()
    {
        webInterfaceManager.refresh();
        super.disabled();
    }

    public int getWeight()
    {
        return weight;
    }

    public WebLabel getWebLabel()
    {
        return label;
    }

    public WebLabel getTooltip()
    {
        return tooltip;
    }

    public void setWebInterfaceManager(final WebInterfaceManager webInterfaceManager)
    {
        this.webInterfaceManager = webInterfaceManager;
        this.moduleDescriptorHelper = new ModuleDescriptorHelper(plugin, webInterfaceManager.getWebFragmentHelper());
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
}
