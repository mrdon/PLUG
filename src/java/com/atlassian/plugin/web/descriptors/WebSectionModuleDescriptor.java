package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

/**
 * Represents a web section - that is a collection of web items.
 */
public class WebSectionModuleDescriptor extends AbstractWebFragmentModuleDescriptor
{
    private String location;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        location = element.attributeValue("location");
    }

    public String getLocation()
    {
        return location;
    }
}
