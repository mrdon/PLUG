package com.atlassian.plugin.webresource;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private List<String> dependencies = new ArrayList<String>();

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        @SuppressWarnings("unchecked")
        List<Element> dependencyElements = element.elements("dependency");
        for (Element dependency : dependencyElements)
        {
            String webResourceKey = dependency.getTextTrim();
            String pluginKey = dependency.attributeValue("plugin");

            // assume dependency on a web resource module in the same plugin if not specified
            if(StringUtils.isBlank(pluginKey))
            {
                pluginKey = plugin.getKey();
            }

            dependencies.add(pluginKey + ":" + webResourceKey);
        }
    }

    /**
     * As this descriptor just handles resources, you should never call this
     */
    @Override
    public Void getModule()
    {
        throw new UnsupportedOperationException("There is no module for Web Resources");
    }

    /**
     * Returns a list of dependencies on other web resources.
     * @return a list of module complete keys
     */
    public List<String> getDependencies()
    {
        return dependencies;
    }
}
