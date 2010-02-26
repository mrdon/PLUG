package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A way of linking to web 'resources', such as javascript or css.  This allows us to include resources once
 * on any given page, as well as ensuring that plugins can declare resources, even if they are included
 * at the bottom of a page.
 */
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private List<String> dependencies = Collections.EMPTY_LIST;
    private boolean disableMinification;
    private Set<String> contexts = Collections.EMPTY_SET;

    public WebResourceModuleDescriptor(ModuleClassFactory moduleCreator)
    {
        super(moduleCreator);
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        final List<String> deps = new ArrayList<String>();
        for (Element dependency : (List<Element>) element.elements("dependency"))
        {
            deps.add(dependency.getTextTrim());
        }
        dependencies = Collections.unmodifiableList(deps);

        final Set<String> ctxs = new HashSet<String>();
        for (Element contextElement : (List<Element>) element.elements("context"))
        {
            ctxs.add(contextElement.getTextTrim());
        }
        contexts = Collections.unmodifiableSet(ctxs);

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
     * Returns the web resource contexts this resource is associated with.
     *
     * @return  the web resource contexts this resource is associated with.
     */
    public Set<String> getContexts()
    {
        return contexts;
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
