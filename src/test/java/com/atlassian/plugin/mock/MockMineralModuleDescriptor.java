package com.atlassian.plugin.mock;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

public class MockMineralModuleDescriptor extends AbstractModuleDescriptor
{
    String weight;

    public void init(Element element) throws PluginParseException
    {
        super.init(element);
        weight = element.element("weight").getTextTrim();
    }

    public Object getModule()
    {
        return new MockGold(Integer.parseInt(weight));
    }


}