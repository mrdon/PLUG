package com.atlassian.plugin;

public interface ModuleDescriptorFactory
{
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    public boolean hasModuleDescriptor(String type);

    public Class getModuleDescriptorClass(String type);
}
