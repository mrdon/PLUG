package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.ResourcedModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

public class MockAnimalModuleDescriptor extends ResourcedModuleDescriptor
{
    Object module;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        try
        {
            module = getModuleClass().newInstance();
        }
        catch (InstantiationException e)
        {
            throw new PluginParseException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new PluginParseException(e);
        }
    }

    public Object getModule()
    {
        return module;
    }
}