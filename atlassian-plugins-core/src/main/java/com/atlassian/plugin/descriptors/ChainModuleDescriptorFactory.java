package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

/**
 * Module descriptor factory that checks multiple factories in sequence.  There is no attempt at caching the results.
 * @since 2.1
 */
public class ChainModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private final ModuleDescriptorFactory[] factories;

    public ChainModuleDescriptorFactory(ModuleDescriptorFactory... factories)
    {
        this.factories = factories;
    }

    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        ModuleDescriptor descriptor = null;
        for (ModuleDescriptorFactory factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                descriptor = factory.getModuleDescriptor(type);
                break;
            }
        }
        return descriptor;
    }

    public boolean hasModuleDescriptor(String type)
    {
        boolean found = false;
        for (ModuleDescriptorFactory factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                found = true;
                break;
            }
        }
        return found;
    }

    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type)
    {
        Class<? extends ModuleDescriptor> descriptorClass = null;
        for (ModuleDescriptorFactory factory : factories)
        {
            descriptorClass = factory.getModuleDescriptorClass(type);
            if (descriptorClass != null)
                break;
        }
        return descriptorClass;
    }
}
