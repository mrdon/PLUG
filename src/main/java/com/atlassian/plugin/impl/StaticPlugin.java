package com.atlassian.plugin.impl;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.Resourced;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.VersionStringComparator;

import java.io.InputStream;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StaticPlugin implements Plugin, Comparable
{
    private String name;
    private String i18nNameKey;
    private String key;
    private Map modules = new LinkedHashMap();
    private boolean enabledByDefault = true;
    private PluginInformation pluginInformation;
    private boolean enabled;
    private boolean system;
    private Resourced resources;
    private Date dateLoaded = new Date();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getI18nNameKey()
    {
        return i18nNameKey;
    }

    public void setI18nNameKey(String i18nNameKey)
    {
        this.i18nNameKey = i18nNameKey;
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
            {
                result.add(moduleDescriptor);
            }
        }

        return result;
    }

    public boolean isEnabledByDefault()
    {
        return enabledByDefault && (pluginInformation == null || pluginInformation.satisfiesMinJavaVersion());
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

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return resources.getResourceLocation(type, name);
    }

    /**
     * @deprecated
     */
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

    public boolean isBundledPlugin()
    {
        return false;
    }

    public Date getDateLoaded()
    {
        return dateLoaded;
    }

    /**
     * Plugins with the same key are compared by version number, using {@link VersionStringComparator}.
     * If the other plugin has a different key, this method returns <tt>1</tt>.
     *
     * @return <tt>-1</tt> if the other plugin is newer, <tt>0</tt> if equal,
     * <tt>1</tt> if the other plugin is older or has a different plugin key.
     */
    public int compareTo(Object other)
    {
        // If the compared object isn't a plugin, then the current object is greater
        if (!(other instanceof Plugin)) return 1;
        Plugin otherPlugin = (Plugin) other;

        // If the compared plugin doesn't have the same key, the current object is greater
        if (!otherPlugin.getKey().equals(this.getKey())) return 1;

        String thisVersion = cleanVersionString(this.getPluginInformation().getVersion());
        String otherVersion = cleanVersionString(otherPlugin.getPluginInformation().getVersion());

        if (!VersionStringComparator.isValidVersionString(thisVersion)) return -1;
        if (!VersionStringComparator.isValidVersionString(otherVersion)) return -1;

        return new VersionStringComparator().compare(thisVersion, otherVersion);
    }

    private String cleanVersionString(String version)
    {
        if (version == null || version.trim().equals("")) return "0";
        return version.replaceAll(" ", "");
    }

    public boolean isDeleteable()
    {
        return false;
    }


    public void close()
    {

    }
}

