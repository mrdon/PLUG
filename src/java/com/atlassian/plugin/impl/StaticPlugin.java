package com.atlassian.plugin.impl;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.license.LicenseRegistry;
import com.atlassian.license.LicenseTypeStore;

import java.util.*;

public class StaticPlugin implements Plugin
{
    private String name;
    private String key;
    private Map modules = new HashMap();
    private boolean enabledByDefault = true;
    private PluginInformation pluginInformation;
    List resourceDescriptors;
    private boolean enabled;

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
        return enabledByDefault;
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

    public List getResourceDescriptors()
    {
        return resourceDescriptors;
    }

    public void setResourceDescriptors(List resourceDescriptors)
    {
        this.resourceDescriptors = resourceDescriptors;
    }

    public List getResourceDescriptors(String type)
    {
        List typedResourceDescriptors = new LinkedList();

        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type))
            {
                typedResourceDescriptors.add(resourceDescriptor);
            }
        }

        return typedResourceDescriptors;
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type) && resourceDescriptor.getName().equalsIgnoreCase(name))
            {
                return resourceDescriptor;
            }
        }

        return null;
    }

    public LicenseRegistry getLicenseRegistry()
    {
        if (getPluginInformation().getLicenseRegistryLocation() != null && !("").equals(getPluginInformation().getLicenseRegistryLocation()))
        {
            try
            {
                Class licenseRegistryClass = ClassLoaderUtils.loadClass(getPluginInformation().getLicenseRegistryLocation(), Plugin.class);

                return (LicenseRegistry) licenseRegistryClass.newInstance();
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException("Could not load License Registry");
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException("Could not load License Registry");
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException("Could not load License Registry");
            }
        }
        return null;
    }

    public LicenseTypeStore getLicenseTypeStore()
    {
        if (getPluginInformation().getLicenseTypeStoreLocation() != null && !("").equals(getPluginInformation().getLicenseTypeStoreLocation()))
        {
            try
            {
                Class licenseTypeStoreClass = ClassLoaderUtils.loadClass(getPluginInformation().getLicenseTypeStoreLocation(), Plugin.class);

                return (LicenseTypeStore) licenseTypeStoreClass.newInstance();
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException("Could not load License Store");
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException("Could not load License Store");
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException("Could not load License Store");
            }
        }
        return null;
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
}

