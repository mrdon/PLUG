package com.atlassian.plugin.servlet.descriptors;

import java.util.Comparator;

import javax.servlet.Filter;

import org.dom4j.Element;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterLocation;

/**
 * A module descriptor that allows plugin developers to define servlet filters.  Developers can define what urls the 
 * filter should be applied to by defining one or more &lt;url-pattern&gt; elements and they can decide where in the
 * filter stack a plugin filter should go by defining the "location" and "weight" attributes. 
 * <p/>
 * The location attribute can have one of four values:
 * </p>
 * <ul>
 * <li>after-encoding - after the character encoding filter</li>
 * <li>before-login - before the login filter</li>
 * <li>before-decoration - before any global decoration like sitemesh</li>
 * <li>before-dispatch - before any dispatching filters or servlets</li>
 * </ul>
 * <p>
 * The default for the location attribute is "before-dispatch".
 * <p/>
 * The weight attribute can have any integer value.  Filters with lower values of the weight attribute will come before
 * those with higher values within the same location.
 *
 * @since 2.1.0
 */
public abstract class ServletFilterModuleDescriptor extends BaseServletModuleDescriptor<Filter> implements StateAware
{
    static final String DEFAULT_LOCATION = FilterLocation.BEFORE_DISPATCH.name();
    static final String DEFAULT_WEIGHT = "100";
    
    private FilterLocation location;
    private int weight;
    
    public static final Comparator<ServletFilterModuleDescriptor> byWeight = new Comparator<ServletFilterModuleDescriptor>()
    {
        public int compare(ServletFilterModuleDescriptor lhs, ServletFilterModuleDescriptor rhs)
        {
            return Integer.valueOf(lhs.getWeight()).compareTo(rhs.getWeight());
        }
    };
    
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        try
        {
            location = FilterLocation.parse(element.attributeValue("location", DEFAULT_LOCATION));
            weight = Integer.valueOf(element.attributeValue("weight", DEFAULT_WEIGHT));
        }
        catch (IllegalArgumentException ex)
        {
            throw new PluginParseException(ex);
        }
    }
    
    public void enabled()
    {
        super.enabled();
        getServletModuleManager().addFilterModule(this);
    }

    public void disabled()
    {
        getServletModuleManager().removeFilterModule(this);
        super.disabled();
    }

    @Override
    public Filter getModule()
    {
        Filter filter = null;
        try
        {
            // Give the plugin a go first
            if (plugin instanceof AutowireCapablePlugin)
                filter = ((AutowireCapablePlugin)plugin).autowire(getModuleClass());
            else
            {
                filter = getModuleClass().newInstance();
                autowireObject(filter);
            }
        }
        catch (InstantiationException e)
        {
            log.error("Error instantiating: " + getModuleClass(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Error accessing: " + getModuleClass(), e);
        }
        return filter;
    }

    public FilterLocation getLocation()
    {
        return location;
    }

    public int getWeight()
    {
        return weight;
    }

    /**
     * Autowire an object. Implement this in your IoC framework or simply do nothing.
     */
    protected abstract void autowireObject(Object obj);

    /**
     * Retrieve the DefaultServletModuleManager class from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();
}
