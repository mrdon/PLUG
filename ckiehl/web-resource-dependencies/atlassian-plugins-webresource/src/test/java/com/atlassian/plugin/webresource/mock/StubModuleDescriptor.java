package com.atlassian.plugin.webresource.mock;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

/**
 *
 */
public class StubModuleDescriptor extends AbstractModuleDescriptor implements StateAware
{
    private Object module;
    private boolean enabled;

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

    public void enabled()
    {
        enabled = true;
    }

    public void disabled()
    {
        enabled = false;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

}
