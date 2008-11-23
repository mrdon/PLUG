package com.atlassian.plugin;

public interface ModuleDescriptorFactory<T, M extends ModuleDescriptor<T>>
{
    ModuleDescriptor<T> getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    boolean hasModuleDescriptor(String type);

    Class<M> getModuleDescriptorClass(String type);
}
