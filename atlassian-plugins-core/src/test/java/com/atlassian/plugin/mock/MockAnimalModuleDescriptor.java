package com.atlassian.plugin.mock;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import org.dom4j.Element;

public class MockAnimalModuleDescriptor extends AbstractModuleDescriptor implements StateAware
{
    Object module;
    public boolean disabled;
    public boolean enabled;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
    }

    public Object getModule()
    {
        if (module == null)
        {
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
        return module;
    }

    public void enabled()
    {
        super.enabled();
        enabled = true;
    }

    public void disabled()
    {
        disabled = true;
    }

    public boolean isEnabled()
    {
        return enabled && !disabled;
    }
}
