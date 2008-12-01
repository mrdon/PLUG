package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;

/**
 * Module descriptor factory that checks multiple factories in sequence.  There is no attempt at caching the results.
 * @since 2.1
 */
public class ChainModuleDescriptorFactory<T, M extends ModuleDescriptor<T>> implements ModuleDescriptorFactory<T, M>
{
    private final ModuleDescriptorFactory<T, M>[] factories;

    public ChainModuleDescriptorFactory(final ModuleDescriptorFactory<T, M>... factories)
    {
        this.factories = factories;
    }

    public M getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        for (final ModuleDescriptorFactory<T, M> factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                return factory.getModuleDescriptor(type);
            }
        }
        return null;
    }

    public boolean hasModuleDescriptor(final String type)
    {
        for (final ModuleDescriptorFactory<T, M> factory : factories)
        {
            if (factory.hasModuleDescriptor(type))
            {
                return true;
            }
        }
        return false;
    }

    public Class<M> getModuleDescriptorClass(final String type)
    {
        for (final ModuleDescriptorFactory<T, M> factory : factories)
        {
            final Class<M> descriptorClass = factory.getModuleDescriptorClass(type);
            if (descriptorClass != null)
            {
                return descriptorClass;
            }
        }
        return null;
    }
}
