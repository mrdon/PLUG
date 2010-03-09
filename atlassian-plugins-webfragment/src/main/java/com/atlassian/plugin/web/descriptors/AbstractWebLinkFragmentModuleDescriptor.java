package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.model.DefaultWebLabel;
import com.atlassian.plugin.web.model.DefaultWebParam;
import com.atlassian.plugin.web.model.WebLabel;

/**
 * An abstract convenience class for web fragment descriptors.
 */
public abstract class AbstractWebLinkFragmentModuleDescriptor<T> extends AbstractWebFragmentModuleDescriptor<T> implements WebLinkFragmentModuleDescriptor<T>
{

    protected DefaultWebLabel label;
    protected DefaultWebLabel tooltip;

    protected AbstractWebLinkFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager)
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY, webInterfaceManager);
    }

    public AbstractWebLinkFragmentModuleDescriptor()
    {
        super(ModuleFactory.LEGACY_MODULE_FACTORY, null);
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

            condition = makeConditions(element, COMPOSITE_TYPE_AND);
        }
        catch (final PluginParseException e)
        {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web fragment", e);
        }

        webInterfaceManager.refresh();
    }

    public WebLabel getWebLabel()
    {
        return label;
    }

    public WebLabel getTooltip()
    {
        return tooltip;
    }
}
