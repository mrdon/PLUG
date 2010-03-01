package com.atlassian.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;

public class MockModuleDescriptor<T> extends AbstractModuleDescriptor<T>
{
    private T module;
    private String key;
    private Plugin plugin;

    public MockModuleDescriptor(Plugin plugin, String key, T module)
    {
        super(ModuleClassFactory.LEGACY_MODULE_CLASS_FACTORY);
        this.module = module;
        this.plugin = plugin;
        this.key = key;
    }

    public T getModule()
    {
        return module;
    }

    @Override
    public String getCompleteKey()
    {
        return plugin.getKey() + ":" + key;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public Plugin getPlugin()
    {
        return plugin;
    }

    @Override
    public Class<T> getModuleClass()
    {
        return (Class<T>) module.getClass();
    }
}
