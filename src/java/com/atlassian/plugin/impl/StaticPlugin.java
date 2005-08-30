package com.atlassian.plugin.impl;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.*;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.util.*;
import java.io.InputStream;

public class StaticPlugin implements Plugin
{
    private String name;
    private String key;
    private Map modules = new HashMap();
    private boolean enabledByDefault = true;
    private PluginInformation pluginInformation;
    List resourceDescriptors;
    private boolean enabled;
    private boolean system;
    private Resourced resources;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String aPackage)
    {
        this.key = aPackage;
    }

    public void addModuleDescriptor(ModuleDescriptor moduleDescriptor)
    {
        modules.put(moduleDescriptor.getKey(), moduleDescriptor);
    }

    public Collection getModuleDescriptors()
    {
        return modules.values();
    }

    public ModuleDescriptor getModuleDescriptor(String key)
    {
        return (ModuleDescriptor) modules.get(key);
    }

    public List getModuleDescriptorsByModuleClass(Class aClass)
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
        return enabledByDefault && pluginInformation.satisfiesMinJavaVersion();
    }

    public void setEnabledByDefault(boolean enabledByDefault)
    {
        this.enabledByDefault = enabledByDefault;
    }

    public PluginInformation getPluginInformation()
    {
        return pluginInformation;
    }

    public void setPluginInformation(PluginInformation pluginInformation)
    {
        this.pluginInformation = pluginInformation;
    }

    public void setResources(Resourced resources)
    {
        this.resources = resources;
    }

    public List getResourceDescriptors()
    {
        return resources.getResourceDescriptors();
    }

    public List getResourceDescriptors(String type)
    {
        return resources.getResourceDescriptors(type);
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return resources.getResourceDescriptor(type, name);
    }

    /**
     * @return true if the plugin has been enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Setter for the enabled state of a plugin. If this is set to
     * false then the plugin will not execute.
     * @param enabled
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Static plugins loaded from the classpath can't be uninstalled.
     */
    public boolean isUninstallable()
    {
        return false;
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return ClassLoaderUtils.loadClass(clazz, callingClass);
    }

    public InputStream getResourceAsStream(String name)
    {
        return ClassLoaderUtils.getResourceAsStream(name, this.getClass());
    }

    public boolean isDynamicallyLoaded()
    {
        return false;
    }

    public boolean isSystemPlugin()
    {
        return system;
    }

    public boolean containsSystemModule()
    {
        for (Iterator iterator = modules.values().iterator(); iterator.hasNext();)
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) iterator.next();
            if(moduleDescriptor.isSystemModule())
            {
                return true;
            }
        }
        return false;
    }

    public void setSystemPlugin(boolean system)
    {
        this.system = system;
    }
}

