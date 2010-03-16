package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.WebInterfaceManager;

public class MockAbstractWebFragmentModuleDescriptor extends AbstractWebFragmentModuleDescriptor<Void>
{
    protected MockAbstractWebFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager)
    {
        super(webInterfaceManager);
    }

    @Override
    public Void getModule()
    {
        return null;
    }
}
