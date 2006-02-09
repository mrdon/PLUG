package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

/**
 * Created by IntelliJ IDEA.
 * User: Jeremy Higgs
 * Date: 6/02/2006
 * Time: 14:36:55
 */

public class UnloadableModuleDescriptor extends AbstractModuleDescriptor
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
    }

    public boolean isEnabledByDefault()
    {
        // An Unloadable module is never enabled
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
}
