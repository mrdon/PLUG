package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebSectionModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WeightedDescriptorComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Stores and manages flexible web interface sections available in the system.
 */
public class DefaultWebInterfaceManager implements WebInterfaceManager
{
    private PluginAccessor pluginAccessor;
    private WebFragmentHelper webFragmentHelper;
    private Map<String,List<WebSectionModuleDescriptor>> sections;
    private Map<String,List<WebItemModuleDescriptor>> items;
    private static final Log log = LogFactory.getLog(DefaultWebInterfaceManager.class);

    public static final WeightedDescriptorComparator WEIGHTED_DESCRIPTOR_COMPARATOR = new WeightedDescriptorComparator();

    public DefaultWebInterfaceManager()
    {
        refresh();
    }

    public DefaultWebInterfaceManager(PluginAccessor pluginAccessor, WebFragmentHelper webFragmentHelper)
    {
        this.pluginAccessor = pluginAccessor;
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
            return Collections.emptyList();
        }

        List<WebSectionModuleDescriptor> result = sections.get(location);

        if (result == null)
        {
            result = new ArrayList<WebSectionModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebSectionModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(WebSectionModuleDescriptor.class);
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
            return Collections.emptyList();
        }

        List<WebItemModuleDescriptor> result = items.get(section);

        if (result == null)
        {
            result = new ArrayList<WebItemModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebItemModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
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
                log.error("Could not evaluate condition '" + descriptor.getCondition() + "' for descriptor: " + descriptor, t);
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

    /**
     * @deprecated since 2.2.0, use {@link #setPluginAccessor(PluginAccessor)} instead
     * @param pluginManager
     */
    @Deprecated
    public void setPluginManager(PluginManager pluginManager)
    {
        setPluginAccessor(pluginManager);
    }

    /**
     * @param pluginAccessor The plugin accessor to set
     * @since 2.2.0
     */
    public void setPluginAccessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
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
