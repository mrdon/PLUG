package com.atlassian.plugin.impl;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.Resourced;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.net.URL;
import java.io.InputStream;

import org.apache.commons.lang.Validate;

/**
 * Delegating plugin that supports easy wrapping
 *
 * @since 2.2.0
 */
public abstract class AbstractDelegatingPlugin implements Plugin, Comparable<Plugin>
{
    private final Plugin delegate;

    public AbstractDelegatingPlugin(Plugin delegate)
    {
        Validate.notNull(delegate);
        this.delegate = delegate;
    }

    public int getPluginsVersion()
    {
        return delegate.getPluginsVersion();
    }

    public void setPluginsVersion(int version)
    {
        delegate.setPluginsVersion(version);
    }

    public String getName()
    {
        return delegate.getName();
    }

    public void setName(String name)
    {
        delegate.setName(name);
    }

    public String getI18nNameKey()
    {
        return delegate.getI18nNameKey();
    }

    public void setI18nNameKey(String i18nNameKey)
    {
        delegate.setI18nNameKey(i18nNameKey);
    }

    public String getKey()
    {
        return delegate.getKey();
    }

    public void setKey(String aPackage)
    {
        delegate.setKey(aPackage);
    }

    public void addModuleDescriptor(ModuleDescriptor<?> moduleDescriptor)
    {
        delegate.addModuleDescriptor(moduleDescriptor);
    }

    public Collection<ModuleDescriptor<?>> getModuleDescriptors()
    {
        return delegate.getModuleDescriptors();
    }

    public ModuleDescriptor<?> getModuleDescriptor(String key)
    {
        return delegate.getModuleDescriptor(key);
    }

    public <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(Class<M> moduleClass)
    {
        return delegate.getModuleDescriptorsByModuleClass(moduleClass);
    }

    public boolean isEnabledByDefault()
    {
        return delegate.isEnabledByDefault();
    }

    public void setEnabledByDefault(boolean enabledByDefault)
    {
        delegate.setEnabledByDefault(enabledByDefault);
    }

    public PluginInformation getPluginInformation()
    {
        return delegate.getPluginInformation();
    }

    public void setPluginInformation(PluginInformation pluginInformation)
    {
        delegate.setPluginInformation(pluginInformation);
    }

    public void setResources(Resourced resources)
    {
        delegate.setResources(resources);
    }

    public PluginState getPluginState()
    {
        return delegate.getPluginState();
    }

    public boolean isEnabled()
    {
        return delegate.isEnabled();
    }

    public boolean isSystemPlugin()
    {
        return delegate.isSystemPlugin();
    }

    public boolean containsSystemModule()
    {
        return delegate.containsSystemModule();
    }

    public void setSystemPlugin(boolean system)
    {
        delegate.setSystemPlugin(system);
    }

    public boolean isBundledPlugin()
    {
        return delegate.isBundledPlugin();
    }

    public Date getDateLoaded()
    {
        return delegate.getDateLoaded();
    }

    public boolean isUninstallable()
    {
        return delegate.isUninstallable();
    }

    public boolean isDeleteable()
    {
        return delegate.isDeleteable();
    }

    public boolean isDynamicallyLoaded()
    {
        return delegate.isDynamicallyLoaded();
    }

    public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
    {
        return delegate.loadClass(clazz, callingClass);
    }

    public ClassLoader getClassLoader()
    {
        return delegate.getClassLoader();
    }

    public URL getResource(String path)
    {
        return delegate.getResource(path);
    }

    public InputStream getResourceAsStream(String name)
    {
        return delegate.getResourceAsStream(name);
    }

    public void setEnabled(boolean enabled)
    {
        delegate.setEnabled(enabled);
    }

    public void close()
    {
        delegate.close();
    }

    public void install()
    {
        delegate.install();
    }

    public void uninstall()
    {
        delegate.uninstall();
    }

    public void enable()
    {
        delegate.enable();
    }

    public void disable()
    {
        delegate.disable();
    }

    public Set<String> getRequiredPlugins()
    {
        return delegate.getRequiredPlugins();
    }

    public List<ResourceDescriptor> getResourceDescriptors()
    {
        return delegate.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(String type)
    {
        return delegate.getResourceDescriptors(type);
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return delegate.getResourceDescriptor(type, name);
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return delegate.getResourceLocation(type, name);
    }

    public int compareTo(Plugin o)
    {
        return delegate.compareTo(o);
    }

    public Plugin getDelegate()
    {
        return delegate;
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return delegate.equals(obj);
    }
}
