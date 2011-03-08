package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import com.atlassian.plugin.web.descriptors.ConditionElementParser;
import com.atlassian.plugin.web.descriptors.ConditionalDescriptor;
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
public class WebResourceModuleDescriptor extends AbstractModuleDescriptor<Void> implements ConditionalDescriptor
{
    private List<String> dependencies = Collections.emptyList();
    private boolean disableMinification;
    private Set<String> contexts = Collections.emptySet();
    private List<WebResourceTransformation> webResourceTransformations = Collections.emptyList();
    private ConditionElementParser conditionElementParser;
    private Element element;
    private Condition condition;

    public WebResourceModuleDescriptor(final HostContainer hostContainer)
    {
        this.conditionElementParser = new ConditionElementParser(new ConditionElementParser.ConditionFactory()
        {
            public Condition create(String className, Plugin plugin) throws ConditionLoadingException
            {
                try
                {
                    Class<Condition> conditionClass = plugin.loadClass(className, this.getClass());
                    return hostContainer.create(conditionClass);
                }
                catch (ClassNotFoundException e)
                {
                    throw new ConditionLoadingException("Cannot load condition class: " + className, e);
                }
            }
        });
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

        final List<WebResourceTransformation> trans = new ArrayList<WebResourceTransformation>();
        for (Element e : (List<Element>) element.elements("transformation"))
        {
            trans.add(new WebResourceTransformation(e));
        }
        webResourceTransformations = Collections.unmodifiableList(trans);

        final Attribute minifiedAttribute = element.attribute("disable-minification");
        disableMinification = minifiedAttribute == null ? false : Boolean.valueOf(minifiedAttribute.getValue());
        this.element = element;
    }

    /**
     * As this descriptor just handles resources, you should never call this
     */
    @Override
    public Void getModule()
    {
        throw new UnsupportedOperationException("There is no module for Web Resources");
    }

    @Override
    public void enabled()
    {
        super.enabled();
        try
        {
            condition = conditionElementParser.makeConditions(plugin, element, ConditionElementParser.CompositeType.AND);
        }
        catch (final PluginParseException e)
        {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web resource due to issue processing condition", e);
        }
    }

    @Override
    public void disabled()
    {
        super.disabled();
        condition = null;
    }

    /**
     * Returns the web resource contexts this resource is associated with.
     *
     * @return  the web resource contexts this resource is associated with.
     * @since 2.5.0
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

    public List<WebResourceTransformation> getTransformations()
    {
        return webResourceTransformations;
    }

    /**
     * @return <code>true</code> if resource minification should be skipped, <code>false</code> otherwise.
     */
    public boolean isDisableMinification()
    {
        return disableMinification;
    }

    /**
     * @return The condition to determine if it should be displayed or not
     * @since 2.7.0
     */
    public Condition getCondition()
    {
        return condition;
    }

    /**
     * @return True if this web resource should be displayed based on the optional condition
     * @since 2.7.0
     */
    public boolean shouldDisplay()
    {
        return condition != null ? condition.shouldDisplay(Collections.<String, Object>emptyMap()) : true;
    }
}
