package com.atlassian.plugin;

import java.util.*;

public class Plugin
{
    private String name;
    private String description;
    private String key;
    private Map modules = new HashMap();
    private boolean enabledByDefault = true;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String aPackage)
    {
        this.key = aPackage;
    }

    public void addModule(ModuleDescriptor moduleDescriptor)
    {
        modules.put(moduleDescriptor.getKey(), moduleDescriptor);
    }

    public Collection getModules()
    {
        return modules.values();
    }

    public ModuleDescriptor getModule(String key)
    {
        return (ModuleDescriptor) modules.get(key);
    }

    public List getModulesByClass(Class aClass)
    {
        List result = new ArrayList();

        for (Iterator iterator = modules.values().iterator(); iterator.hasNext();)
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator.next();

            Class moduleClass = moduleDescriptor.getModuleClass();
            if (aClass.isAssignableFrom(moduleClass))
                result.add(moduleDescriptor.getModule());
        }

        return result;
    }

    public List getModuleDescriptorsByClass(Class aClass)
    {
        List result = new ArrayList();

        for (Iterator iterator = modules.values().iterator(); iterator.hasNext();)
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator.next();

            Class moduleClass = moduleDescriptor.getModuleClass();
            if (aClass.isAssignableFrom(moduleClass))
                result.add(moduleDescriptor);
        }

        return result;
    }

    public boolean isEnabledByDefault()
    {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault)
    {
        this.enabledByDefault = enabledByDefault;
    }
}
