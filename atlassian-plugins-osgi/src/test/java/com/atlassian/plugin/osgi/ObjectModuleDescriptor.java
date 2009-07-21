package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.AutowireCapablePlugin;

/**
 * Module type for an object
 */
public class ObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    public Object getModule()
    {
        Object module = null;
        // Give the plugin a go first
        if (plugin instanceof AutowireCapablePlugin)
        {
            module = ((AutowireCapablePlugin) plugin).autowire(getModuleClass());
        }
        else
        {
            try
            {
                module = getModuleClass().newInstance();
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }
        return module;
    }
}
