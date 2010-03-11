package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebInterfaceManager;

public class MockAbstractWebLinkFragmentModuleDescriptor extends AbstractWebLinkFragmentModuleDescriptor
{
    protected MockAbstractWebLinkFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager)
    {
        super(webInterfaceManager);
    }

    @Override
    public Void getModule()
    {
        return null;
    }
}
