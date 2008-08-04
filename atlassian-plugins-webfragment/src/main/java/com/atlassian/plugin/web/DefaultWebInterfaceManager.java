package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.web.descriptors.*;

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
    private Map<String,List<WebSectionModuleDescriptor>> sections;
    private Map<String,List<WebItemModuleDescriptor>> items;
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

    public List<WebSectionModuleDescriptor> getSections(String location)
    {
        if (location == null)
        {
            return Collections.EMPTY_LIST;
        }

        List<WebSectionModuleDescriptor> result = sections.get(location);

        if (result == null)
        {
            result = new ArrayList<WebSectionModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebSectionModuleDescriptor> descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebSectionModuleDescriptor.class);
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

    public List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String,Object> context)
    {
        return filterFragmentsByCondition(getSections(location), context);
    }

    public List<WebItemModuleDescriptor> getItems(String section)
    {
        if (section == null)
        {
            return Collections.EMPTY_LIST;
        }

        List<WebItemModuleDescriptor> result = items.get(section);

        if (result == null)
        {
            result = new ArrayList<WebItemModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebItemModuleDescriptor> descriptors = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
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

    public List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String,Object> context)
    {
        return filterFragmentsByCondition(getItems(section), context);
    }

    private <T extends WebFragmentModuleDescriptor> List<T> filterFragmentsByCondition(List<T> relevantItems, Map<String,Object> context)
    {
        if (relevantItems.isEmpty())
        {
            return relevantItems;
        }

        List<T> result = new ArrayList<T>(relevantItems);
        for (Iterator iterator = result.iterator(); iterator.hasNext();)
        {
            WebFragmentModuleDescriptor descriptor = (WebFragmentModuleDescriptor) iterator.next();
            try
            {
                if (descriptor.getCondition() != null && !descriptor.getCondition().shouldDisplay(context))
                {
                    iterator.remove();
                }
            }
            catch (Throwable t)
            {
                log.error("Could not evaluate condition for descriptor: " + descriptor + ", with throwable: " + t);
                iterator.remove();
            }
        }

        return result;
    }

    public void refresh()
    {
        sections = Collections.synchronizedMap(new HashMap());
        items = Collections.synchronizedMap(new HashMap());
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
