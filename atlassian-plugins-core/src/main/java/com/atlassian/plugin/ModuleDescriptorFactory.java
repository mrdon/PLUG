package com.atlassian.plugin;

public interface ModuleDescriptorFactory
{
    <M> ModuleDescriptor<M> getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    <M, D extends ModuleDescriptor<M>> Class<D> getModuleDescriptorClass(String type);

    boolean hasModuleDescriptor(String type);
}
