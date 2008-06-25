package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import org.dom4j.Element;

public class MockMineralModuleDescriptor extends AbstractModuleDescriptor
{
    String weight;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        if (element.element("weight") != null)
            weight = element.element("weight").getTextTrim();
    }

    public Object getModule()
    {
        return new MockGold(Integer.parseInt(weight));
    }


}