package com.atlassian.plugin.osgi;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

import java.util.concurrent.Callable;

public class CallableModuleDescriptor extends AbstractModuleDescriptor<Callable>
{
    public CallableModuleDescriptor(ModuleFactory moduleCreator)
    {
        super(moduleCreator);
    }
    
    @Override
    public Callable getModule()
    {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
