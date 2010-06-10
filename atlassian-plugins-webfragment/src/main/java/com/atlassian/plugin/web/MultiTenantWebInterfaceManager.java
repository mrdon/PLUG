package com.atlassian.plugin.web;

import com.atlassian.multitenant.MultiTenantAwareComponentCreator;
import com.atlassian.multitenant.MultiTenantAwareComponentMap;
import com.atlassian.multitenant.MultiTenantContext;
import com.atlassian.multitenant.MultiTenantDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.MultiTenantPluginDisabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginEnabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.MultiTenantPluginModuleEnabledEvent;
import com.atlassian.plugin.web.descriptors.WebItemModuleDescriptor;
import com.atlassian.plugin.web.descriptors.WebSectionModuleDescriptor;

import java.util.List;
import java.util.Map;

public class MultiTenantWebInterfaceManager implements WebInterfaceManager
{
    private final MultiTenantAwareComponentMap<WebInterfaceManager> map;
    private final PluginEventManager pluginEventManager;

    public MultiTenantWebInterfaceManager(final PluginAccessor pluginAccessor, final WebFragmentHelper webFragmentHelper,
            PluginEventManager pluginEventManager)
    {
        this.pluginEventManager = pluginEventManager;
        map = MultiTenantContext.getFactory().createComponentMap(new MultiTenantAwareComponentCreator<WebInterfaceManager>()
        {
            public WebInterfaceManager create(MultiTenantDescriptor descriptor)
            {
                return new DefaultWebInterfaceManager(pluginAccessor, webFragmentHelper);
            }
        });
        pluginEventManager.register(this);
    }

    public boolean hasSectionsForLocation(String location)
    {
        return getWebInterfaceManager().hasSectionsForLocation(location);
    }

    public List<WebSectionModuleDescriptor> getSections(String location)
    {
        return getWebInterfaceManager().getSections(location);
    }

    public List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context)
    {
        return getWebInterfaceManager().getDisplayableSections(location, context);
    }

    public List<WebItemModuleDescriptor> getItems(String section)
    {
        return getWebInterfaceManager().getItems(section);
    }

    public List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context)
    {
        return getWebInterfaceManager().getDisplayableItems(section, context);
    }

    public void refresh()
    {
        for (WebInterfaceManager manager : map.getAll())
        {
            manager.refresh();
        }
    }

    // Refresh whenever a plugin or module is disabled for a tenant

    @PluginEventListener
    public void pluginEnabled(MultiTenantPluginEnabledEvent event)
    {
        getWebInterfaceManager().refresh();
    }

    @PluginEventListener
    public void pluginDisabled(MultiTenantPluginDisabledEvent event)
    {
        getWebInterfaceManager().refresh();
    }

    @PluginEventListener
    public void moduleEnabled(MultiTenantPluginModuleEnabledEvent event)
    {
        getWebInterfaceManager().refresh();
    }

    @PluginEventListener
    public void moduleDisabled(MultiTenantPluginModuleDisabledEvent event)
    {
        getWebInterfaceManager().refresh();
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return getWebInterfaceManager().getWebFragmentHelper();
    }

    private WebInterfaceManager getWebInterfaceManager()
    {
        return map.get();
    }

}
