package com.atlassian.plugin;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.loaders.SinglePluginLoader;

import java.util.Map;
import java.util.HashMap;

public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private Map moduldeDescriptorClasses;

    public DefaultModuleDescriptorFactory()
    {
        this.moduldeDescriptorClasses = new HashMap();
    }

    public Class getModuleDescriptorClass(String type)
    {
        return (Class) moduldeDescriptorClasses.get(type);
    }

    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        Class moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null)
            throw new PluginParseException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");

        return (ModuleDescriptor) ClassLoaderUtils.loadClass(moduleDescriptorClazz.getName(), SinglePluginLoader.class).newInstance();
    }

    public boolean hasModuleDescriptor(String type)
    {
        return moduldeDescriptorClasses.containsKey(type);
    }

    public void addModuleDescriptor(String type, Class moduleDescriptorClass)
    {
        moduldeDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(String type)
    {
        moduldeDescriptorClasses.remove(type);
    }

    protected Map getDescriptorClassesMap()
    {
        return moduldeDescriptorClasses;
    }
}
