package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Resources;
import com.atlassian.plugin.elements.ResourceDescriptor;
import org.dom4j.Element;

import java.util.Collections;

public class UnrecognisedModuleDescriptor extends AbstractModuleDescriptor
{
    private String errorText;

    public Object getModule()
    {
        return null;
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        this.key = element.attributeValue("key");
        this.name = element.attributeValue("name");
        this.description = element.elementTextTrim("description");

        this.plugin = plugin;
        this.resources = new Resources(Collections.<ResourceDescriptor>emptyList());
    }

    @Override
    public boolean isEnabledByDefault()
    {
        //never enable a UnrecognisedModuleDescriptor
        return false;
    }

    public String getErrorText()
    {
        return errorText;
    }

    public void setErrorText(String errorText)
    {
        this.errorText = errorText;
    }

    /**
     * Sets the key of the ModuleDescriptor
     *
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnrecognisedModuleDescriptor when we don't have the XML Element.
     *
     * @param key the key of the ModuleDescriptor
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * Sets the name of the ModuleDescriptor
     *
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnrecognisedModuleDescriptor when we don't have the XML Element.
     *
     * @param name the name of the ModuleDescriptor
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
