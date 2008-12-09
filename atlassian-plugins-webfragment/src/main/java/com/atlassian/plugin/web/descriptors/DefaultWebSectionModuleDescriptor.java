package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.WebInterfaceManager;

import org.dom4j.Element;

/**
 * Represents a web section - that is a collection of web items.
 */
public class DefaultWebSectionModuleDescriptor extends AbstractWebFragmentModuleDescriptor<Void> implements WebSectionModuleDescriptor<Void>
{
    private String location;

    public DefaultWebSectionModuleDescriptor(final WebInterfaceManager webInterfaceManager)
    {
        super(webInterfaceManager);
    }

    public DefaultWebSectionModuleDescriptor()
    {

    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        location = element.attributeValue("location");
    }

    public String getLocation()
    {
        return location;
    }

    @Override
    public Class<Void> getModuleClass()
    {
        return Void.class;
    }

    @Override
    public Void getModule()
    {
        return null;
    }
}
