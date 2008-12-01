package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;

/**
 * Module descriptor factory that checks multiple factories in sequence.  There is no attempt at caching the results.
 * @since 2.1
 */
public class ChainModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private final ModuleDescriptorFactory[] factories;

    public ChainModuleDescriptorFactory(final ModuleDescriptorFactory... factories)
    {
        this.factories = factories;
    }

    public <M> ModuleDescriptor<M> getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        for (final ModuleDescriptorFactory factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                return factory.<M> getModuleDescriptor(type);
            }
        }
        return null;
    }

    public boolean hasModuleDescriptor(final String type)
    {
        for (final ModuleDescriptorFactory factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                return true;
            }
        }
        return false;
    }

    public <M, D extends ModuleDescriptor<M>> Class<D> getModuleDescriptorClass(final String type)
    {
        for (final ModuleDescriptorFactory factory : factories)
        {
            final Class<D> descriptorClass = factory.<M, D> getModuleDescriptorClass(type);
            if (descriptorClass != null)
            {
                return descriptorClass;
            }
        }
        return null;
    }
}
