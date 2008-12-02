package com.atlassian.plugin.osgi.external;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

import java.util.Collections;
import java.util.Set;

/**
 * Single module descriptor factory for plugins to use when they want to expose just one plugin. Does not support
 * autowiring module descriptors. 
 *
 * @since 2.1
 */
public class SingleModuleDescriptorFactory<T extends ModuleDescriptor<?>> implements ListableModuleDescriptorFactory
{
    private final String type;
    private final Class<T> moduleDescriptorClass;

    public SingleModuleDescriptorFactory(final String type, final Class<T> moduleDescriptorClass)
    {
        this.moduleDescriptorClass = moduleDescriptorClass;
        this.type = type;
    }

    public ModuleDescriptor getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        T result = null;
        if (this.type.equals(type))
        {
            // We can't use an autowired bean factory to create the instance because it would be loaded by this class's
            // classloader, which will not have access to the spring instance in bundle space.
            result = moduleDescriptorClass.newInstance();
        }
        return result;
    }

    public boolean hasModuleDescriptor(final String type)
    {
        return (this.type.equals(type));
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ModuleDescriptor<?>> getModuleDescriptorClass(final String type)
    {
        return (this.type.equals(type) ? moduleDescriptorClass : null);
    }

    @SuppressWarnings("unchecked")
    public Set<Class<ModuleDescriptor<?>>> getModuleDescriptorClasses()
    {
        return Collections.singleton((Class<ModuleDescriptor<?>>) moduleDescriptorClass);
    }
}
