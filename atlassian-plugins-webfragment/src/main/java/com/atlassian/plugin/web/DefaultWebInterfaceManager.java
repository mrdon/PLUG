package com.atlassian.plugin.web;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.descriptors.*;

import java.util.*;

import com.atlassian.plugin.web.model.WebPanel;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and manages flexible web interface sections available in the system.
 */
public class DefaultWebInterfaceManager implements WebInterfaceManager
{
    private PluginAccessor pluginAccessor;
    private WebFragmentHelper webFragmentHelper;
    private Map<String, List<WebSectionModuleDescriptor>> sections;
    private Map<String, List<WebItemModuleDescriptor>> items;
    private Map<String, List<WebPanelModuleDescriptor>> panels;
    private static final Logger log = LoggerFactory.getLogger(DefaultWebInterfaceManager.class);

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
            for (WebSectionModuleDescriptor descriptor : descriptors) {
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
            for (WebItemModuleDescriptor descriptor : descriptors) {
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

    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context)
    {
        return toWebPanels(getDisplayableWebPanelDescriptors(location, context));
    }

    public List<WebPanelModuleDescriptor> getDisplayableWebPanelDescriptors(String location, Map<String, Object> context)
    {
        return filterFragmentsByCondition(getWebPanelDescriptors(location), context);
    }

    public List<WebPanel> getWebPanels(String location)
    {
        return toWebPanels(getWebPanelDescriptors(location));
    }

    private List<WebPanel> toWebPanels(List<WebPanelModuleDescriptor> descriptors)
    {
        return Lists.transform(descriptors, new Function<WebPanelModuleDescriptor, WebPanel>()
        {
            public WebPanel apply(WebPanelModuleDescriptor from)
            {
                return from.getModule();
            }
        });
    }

    public List<WebPanelModuleDescriptor> getWebPanelDescriptors(String location)
    {
        if (location == null)
        {
            return Collections.emptyList();
        }
        else
        {
            List<WebPanelModuleDescriptor> result = panels.get(location);
            if (result == null) {
                result = new ArrayList<WebPanelModuleDescriptor>(); // use a tree map so we get nice weight sorting

                List<WebPanelModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(WebPanelModuleDescriptor.class);
                for (WebPanelModuleDescriptor descriptor : descriptors)
                {
                    if (location.equalsIgnoreCase(descriptor.getLocation()))
                    {
                        result.add(descriptor);
                    }
                }
                Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
                panels.put(location, result);
            }
            return result;
        }
    }

    private <T extends ConditionalDescriptor> List<T> filterFragmentsByCondition(List<T> relevantItems, Map<String,Object> context)
    {
        if (relevantItems.isEmpty())
        {
            return relevantItems;
        }

        List<T> result = new ArrayList<T>(relevantItems);
        for (Iterator<T> iterator = result.iterator(); iterator.hasNext();)
        {
            ConditionalDescriptor descriptor = iterator.next();
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
        sections = Collections.synchronizedMap(new HashMap<String, List<WebSectionModuleDescriptor>>());
        items = Collections.synchronizedMap(new HashMap<String, List<WebItemModuleDescriptor>>());
        panels = Collections.synchronizedMap(new HashMap<String, List<WebPanelModuleDescriptor>>());
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
