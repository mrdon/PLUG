package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.AutowireCapablePlugin;

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
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return module;
    }
}
