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

public abstract class ServletFilterModuleDescriptor extends BaseServletModuleDescriptor<Filter> implements StateAware
{
    private FilterLocation location = FilterLocation.bottom;
    private int weight = 100;
    
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
        String locValue = element.attributeValue("location");
        if (locValue != null)
            location = FilterLocation.valueOf(locValue);

        if (element.attributeValue("weight") != null)
            weight = Integer.valueOf(element.attributeValue("weight"));
    }
    
    public void enabled()
    {
        getServletModuleManager().addFilterModule(this);
    }

    public void disabled()
    {
        getServletModuleManager().removeFilterModule(this);
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
