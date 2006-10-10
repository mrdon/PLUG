package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebSectionModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WeightedDescriptorComparator;

import java.util.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Stores and manages flexible web interface sections available in the system.
 */
public class DefaultWebInterfaceManager implements WebInterfaceManager
{
    private PluginManager pluginManager;
    private WebFragmentHelper webFragmentHelper;
    private Map sections;
    private Map items;
    private static final Log log = LogFactory.getLog(DefaultWebInterfaceManager.class);

    public static final WeightedDescriptorComparator WEIGHTED_DESCRIPTOR_COMPARATOR = new WeightedDescriptorComparator();

    public DefaultWebInterfaceManager()
    {
        refresh();
    }

    public DefaultWebInterfaceManager(PluginManager pluginManager, WebFragmentHelper webFragmentHelper)
    {
        this.pluginManager = pluginManager;
        this.webFragmentHelper = webFragmentHelper;
        refresh();
    }

    public boolean hasSectionsForLocation(String location)
    {
        return !getSections(location).isEmpty();
    }

    public List getSections(String location)
    {
        if (location == null)
        {
            return Collections.EMPTY_LIST;
        }

        List result = (List) sections.get(location);

        if (result == null)
        {
            result = new ArrayList(); // use a tree map so we get nice weight sorting
            List descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebSectionModuleDescriptor.class);
            for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
            {
                WebSectionModuleDescriptor descriptor = (WebSectionModuleDescriptor) iterator.next();
                if (location.equalsIgnoreCase(descriptor.getLocation()))
                    result.add(descriptor);
            }

            Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
            sections.put(location, result);
        }

        return result;
    }

    public List getDisplayableSections(String location, Map context)
    {
        return filterFragmentsByCondition(getSections(location), context);
    }

    public List getItems(String section)
    {
        if (section == null)
        {
            return Collections.EMPTY_LIST;
        }

        List result = (List) items.get(section);

        if (result == null)
        {
            result = new ArrayList(); // use a tree map so we get nice weight sorting
            List descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
            for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
            {
                WebItemModuleDescriptor descriptor = (WebItemModuleDescriptor) iterator.next();
                if (section.equalsIgnoreCase(descriptor.getSection()))
                    result.add(descriptor);
            }

            Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
            items.put(section, result);
        }

        return result;
    }

    public List getDisplayableItems(String section, Map context)
    {
        return filterFragmentsByCondition(getItems(section), context);
    }

    private List filterFragmentsByCondition(List relevantItems, Map context)
    {
        if (relevantItems.isEmpty())
        {
            return relevantItems;
        }

        List result = new ArrayList(relevantItems);
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            AbstractWebFragmentModuleDescriptor descriptor = (AbstractWebFragmentModuleDescriptor) iterator.next();
            try
            {
                if (descriptor.getCondition() != null && !descriptor.getCondition().shouldDisplay(context))
                {
                    iterator.remove();
                }
            }
            catch (Throwable t)
            {
                log.error("Could not evaluate condition: " + t);
                iterator.remove();
            }
        }

        return result;
    }

    public void refresh()
    {
        sections = new HashMap();
        items = new HashMap();
    }

    public void setPluginManager(PluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public void setWebFragmentHelper(WebFragmentHelper webFragmentHelper)
    {
        this.webFragmentHelper = webFragmentHelper;
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return webFragmentHelper;
    }

}
