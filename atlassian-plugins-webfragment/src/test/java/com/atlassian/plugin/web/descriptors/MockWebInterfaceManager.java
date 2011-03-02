package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.model.WebPanel;

import java.util.List;
import java.util.Map;

public class MockWebInterfaceManager implements WebInterfaceManager
{
    public boolean hasSectionsForLocation(String location)
    {
        return false;
    }

    public List<WebSectionModuleDescriptor> getSections(String location)
    {
        return null;
    }

    public List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map context)
    {
        return null;
    }

    public List<WebItemModuleDescriptor> getItems(String section)
    {
        return null;
    }

    public List<WebItemModuleDescriptor> getDisplayableItems(String section, Map context)
    {
        return null;
    }

    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context)
    {
        return null;
    }

    public List<WebPanelModuleDescriptor> getWebPanelDescriptors(String location)
    {
        return null;
    }

    public List<WebPanelModuleDescriptor> getDisplayableWebPanelDescriptors(String location, Map<String, Object> context)
    {
        return null;
    }

    public List<WebPanel> getWebPanels(String location)
    {
        return null;
    }

    public void refresh()
    {
    }

    public WebFragmentHelper getWebFragmentHelper()
    {
        return new MockWebFragmentHelper();
    }
}
