package com.atlassian.plugin;

import com.atlassian.license.LicenseRegistry;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.Collection;
import java.util.List;

public interface Plugin
{
    String getName();

    void setName(String name);

    String getKey();

    void setKey(String aPackage);

    void addModuleDescriptor(ModuleDescriptor moduleDescriptor);

    Collection getModuleDescriptors();

    ModuleDescriptor getModuleDescriptor(String key);

    List getModuleDescriptorsByModuleClass(Class aClass);

    boolean isEnabledByDefault();

    void setEnabledByDefault(boolean enabledByDefault);

    PluginInformation getPluginInformation();

    void setPluginInformation(PluginInformation pluginInformation);

    List getResourceDescriptors();

    void setResourceDescriptors(List resourceDescriptors);

    List getResourceDescriptors(String type);

    ResourceDescriptor getResourceDescriptor(String type, String name);

    LicenseRegistry getLicenseRegistry();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Whether or not this plugin can be 'uninstalled'.
     */
    boolean isUninstallable();

    Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException;
}
