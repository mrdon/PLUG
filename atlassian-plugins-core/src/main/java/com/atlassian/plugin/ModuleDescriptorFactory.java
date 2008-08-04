package com.atlassian.plugin;

public interface ModuleDescriptorFactory
{
    ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    boolean hasModuleDescriptor(String type);

    Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type);
}
