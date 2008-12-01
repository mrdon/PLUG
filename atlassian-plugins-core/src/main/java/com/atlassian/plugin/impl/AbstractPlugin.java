package com.atlassian.plugin.impl;

import static com.atlassian.plugin.util.concurrent.CopyOnWriteMap.newLinkedMap;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.Resourced;
import com.atlassian.plugin.Resources;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.util.VersionStringComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class AbstractPlugin implements Plugin, Comparable<Plugin>
{
    private final Map<String, ModuleDescriptor<?>> modules = newLinkedMap();
    private String name;
    private String i18nNameKey;
    private String key;
    private boolean enabledByDefault = true;
    private PluginInformation pluginInformation = new PluginInformation();
    private boolean enabled;
    private boolean system;
    private Resourced resources = Resources.EMPTY_RESOURCES;
    private int pluginsVersion = 1;
    private final Date dateLoaded = new Date();

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getI18nNameKey()
    {
        return i18nNameKey;
    }

    public void setI18nNameKey(final String i18nNameKey)
    {
        this.i18nNameKey = i18nNameKey;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(final String aPackage)
    {
        key = aPackage;
    }

    public void addModuleDescriptor(final ModuleDescriptor<?> moduleDescriptor)
    {
        modules.put(moduleDescriptor.getKey(), moduleDescriptor);
    }

    protected void removeModuleDescriptor(final String key)
    {
        modules.remove(key);
    }

    /**
     * Returns a copy of the module descriptors for this plugin
     * @return A copy of the internal list
     */
    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors()
    {
        // generics hack
        final Collection<ModuleDescriptor<?>> descriptors = modules.values();
        final ArrayList<ModuleDescriptor<M>> result = new ArrayList<ModuleDescriptor<M>>(descriptors.size());
        for (final ModuleDescriptor<?> moduleDescriptor : descriptors)
        {
            @SuppressWarnings("unchecked")
            final ModuleDescriptor<M> typedDescriptor = (ModuleDescriptor<M>) moduleDescriptor;
            result.add(typedDescriptor);
        }
        return result;
    }

    public <M> ModuleDescriptor<M> getModuleDescriptor(final String key)
    {
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<M> moduleDescriptor = (ModuleDescriptor<M>) modules.get(key);
        return moduleDescriptor;
    }

    public <T> List<ModuleDescriptor<T>> getModuleDescriptorsByModuleClass(final Class<T> aClass)
    {
        final List<ModuleDescriptor<T>> result = new ArrayList<ModuleDescriptor<T>>();
        for (final ModuleDescriptor<?> moduleDescriptor : modules.values())
        {
            final Class<?> moduleClass = moduleDescriptor.getModuleClass();
            if (aClass.isAssignableFrom(moduleClass))
            {
                @SuppressWarnings("unchecked")
                final ModuleDescriptor<T> typedModuleDescriptor = (ModuleDescriptor<T>) moduleDescriptor;
                result.add(typedModuleDescriptor);
            }
        }
        return result;
    }

    public boolean isEnabledByDefault()
    {
        return enabledByDefault && ((pluginInformation == null) || pluginInformation.satisfiesMinJavaVersion());
    }

    public void setEnabledByDefault(final boolean enabledByDefault)
    {
        this.enabledByDefault = enabledByDefault;
    }

    public int getPluginsVersion()
    {
        return pluginsVersion;
    }

    public void setPluginsVersion(final int pluginsVersion)
    {
        this.pluginsVersion = pluginsVersion;
    }

    public PluginInformation getPluginInformation()
    {
        return pluginInformation;
    }

    public void setPluginInformation(final PluginInformation pluginInformation)
    {
        this.pluginInformation = pluginInformation;
    }

    public void setResources(final Resourced resources)
    {
        this.resources = resources != null ? resources : Resources.EMPTY_RESOURCES;
    }

    public List<ResourceDescriptor> getResourceDescriptors()
    {
        return resources.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(final String type)
    {
        return resources.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(final String type, final String name)
    {
        return resources.getResourceLocation(type, name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public ResourceDescriptor getResourceDescriptor(final String type, final String name)
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
     * Setter for the enabled state of a plugin. If this is set to false then the plugin will not execute.
     */
    public void setEnabled(final boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isSystemPlugin()
    {
        return system;
    }

    public boolean containsSystemModule()
    {
        for (final ModuleDescriptor<?> moduleDescriptor : modules.values())
        {
            if (moduleDescriptor.isSystemModule())
            {
                return true;
            }
        }
        return false;
    }

    public void setSystemPlugin(final boolean system)
    {
        this.system = system;
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
    public int compareTo(final Plugin otherPlugin)
    {
        if (otherPlugin.getKey() == null)
        {
            return 1;
        }
        if (getKey() == null)
        {
            return -1;
        }

        // If the compared plugin doesn't have the same key, the current object is greater
        if (!otherPlugin.getKey().equals(getKey()))
        {
            return getKey().compareTo(otherPlugin.getKey());
        }

        final String thisVersion = cleanVersionString((getPluginInformation() != null ? getPluginInformation().getVersion() : null));
        final String otherVersion = cleanVersionString((otherPlugin.getPluginInformation() != null ? otherPlugin.getPluginInformation().getVersion() : null));

        if (!VersionStringComparator.isValidVersionString(thisVersion))
        {
            return -1;
        }
        if (!VersionStringComparator.isValidVersionString(otherVersion))
        {
            return -1;
        }

        return new VersionStringComparator().compare(thisVersion, otherVersion);
    }

    private String cleanVersionString(final String version)
    {
        if ((version == null) || version.trim().equals(""))
        {
            return "0";
        }
        return version.replaceAll(" ", "");
    }

    @Override
    public String toString()
    {
        final PluginInformation info = getPluginInformation();
        return getKey() + ":" + (info == null ? "?" : info.getVersion());
    }
}
