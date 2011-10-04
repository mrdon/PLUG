package com.atlassian.plugin.descriptors;

import org.dom4j.Element;

import com.atlassian.plugin.descriptors.PluginPoint;
import com.atlassian.plugin.module.ModuleFactory;

public class MyPluginPoint extends PluginPoint<Book>
{

    public MyPluginPoint(ModuleFactory moduleFactory)
    {
        super(moduleFactory);
    }

    @Override
    public Class<Book> chooseJaxbClass(Element element)
    {
        return Book.class;
    }
}
