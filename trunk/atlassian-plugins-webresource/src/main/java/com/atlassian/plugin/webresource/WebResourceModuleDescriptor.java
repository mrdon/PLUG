package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private List<String> dependencies = new ArrayList<String>();
    private boolean disableMinification;

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        @SuppressWarnings("unchecked")
        List<Element> dependencyElements = element.elements("dependency");
        for (Element dependency : dependencyElements)
        {
            dependencies.add(dependency.getTextTrim());
        }

        final Attribute minifiedAttribute = element.attribute("disable-minification");
        disableMinification = minifiedAttribute == null ? false : Boolean.valueOf(minifiedAttribute.getValue());
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

    /**
     * @return true if the minified form of the resources should be returned if they exist.
     */
    public boolean isDisableMinification()
    {
        return disableMinification;
    }
}
