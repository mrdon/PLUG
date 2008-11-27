package com.atlassian.plugin.mock;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

import org.dom4j.Element;

public class MockMineralModuleDescriptor extends AbstractModuleDescriptor<MockMineral>
{
    String weight;

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);
        if (element.element("weight") != null)
        {
            weight = element.element("weight").getTextTrim();
        }
    }

    @Override
    public MockMineral getModule()
    {
        return new MockGold(Integer.parseInt(weight));
    }
}