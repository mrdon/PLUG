package com.atlassian.plugin.modulefactory;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * Uses a simple newInstance mechanism to instantiate a module.
 */
public class SimpleModuleFactory implements ModuleFactory
{

    private Class moduleClass;

    public SimpleModuleFactory(Class moduleClass)
    {
        this.moduleClass = moduleClass;
    }

    public Object getModule()
    {
        try
        {
            return moduleClass.newInstance();
        } catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }


    public void setModuleDescriptor(ModuleDescriptor resourced)
    {
        // we don't need resources
    }

}
