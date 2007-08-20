package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.WebFragmentHelper;

import java.util.List;
import java.util.Map;

public class MockWebInterfaceManager implements WebInterfaceManager
{
    public boolean hasSectionsForLocation(String location)
    {
        return false;
    }

    public List getSections(String location)
    {
        return null;
    }

    public List getDisplayableSections(String location, Map context)
    {
        return null;
    }

    public List getItems(String section)
    {
        return null;
    }

    public List getDisplayableItems(String section, Map context)
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
