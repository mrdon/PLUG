package com.atlassian.plugin;

public interface ModuleDescriptorFactory<T, D extends ModuleDescriptor<? extends T>>
{
    D getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    boolean hasModuleDescriptor(String type);

    <C extends D> Class<C> getModuleDescriptorClass(String type);
}
