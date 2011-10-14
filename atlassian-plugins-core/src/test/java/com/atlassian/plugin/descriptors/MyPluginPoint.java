package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.module.ModuleFactory;

public class MyPluginPoint extends JaxbAbstractModuleDescriptor<Book>
{

    public MyPluginPoint(ModuleFactory moduleFactory)
    {
        super(moduleFactory, Book.class);
    }

    @Override
    public void init(Plugin plugin, Book configuration)
    {
        // Nothing to initialise
    }
}
