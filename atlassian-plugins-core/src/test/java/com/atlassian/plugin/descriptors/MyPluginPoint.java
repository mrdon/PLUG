package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.module.ModuleFactory;

public class MyPluginPoint extends JaxbAbstractModuleDescriptor<Book, Void>
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

    @Override
    public Void getModule()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
