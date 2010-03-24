package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import org.dom4j.Element;

/**
 * This class is used for constructing {@link com.atlassian.plugin.web.ContextProvider}
 * objects from a module descriptor's XML element. Its functionality is used
 * by both {@link com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor}
 * and {@link com.atlassian.plugin.web.descriptors.DefaultWebPanelModuleDescriptor}.
 *
 * @since   2.5.0
 */
class ContextProviderElementParser
{
    private final WebFragmentHelper webFragmentHelper;

    public ContextProviderElementParser(WebFragmentHelper webFragmentHelper)
    {
        this.webFragmentHelper = webFragmentHelper;
    }

    /**
     *
     * @param element   the module descriptor's XML element which contains the nested &lt;context-provider> element.
     * @return  the configured {@link ContextProvider} instance, or <code>null</code> when the descriptor does not
     * contain a &lt;context-provider> element.
     * @throws PluginParseException
     */
    public ContextProvider makeContextProvider(Plugin plugin, final Element element) throws PluginParseException
    {
        try
        {
            if (element.element("context-provider") != null)
            {
                final ContextProvider context = webFragmentHelper.loadContextProvider(element.attributeValue("class"), plugin);
                context.init(LoaderUtils.getParams(element));
                return context;
            }
            else
            {
                return null;
            }
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured context-provider class does not implement the ContextProvider interface", e);
        }
        catch (final ConditionLoadingException cle)
        {
            throw new PluginParseException("Unable to load the module's display conditions: " + cle.getMessage(), cle);
        }
    }
}