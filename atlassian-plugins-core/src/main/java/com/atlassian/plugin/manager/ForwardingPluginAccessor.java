package com.atlassian.plugin.manager;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginRestartState;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;
import com.atlassian.plugin.predicate.PluginPredicate;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Simple forwarding delegate for a {@link PluginAccessor}.
 * 
 * @since 2.7.0
 */
abstract class ForwardingPluginAccessor implements PluginAccessor
{
    protected final PluginAccessor delegate;

    ForwardingPluginAccessor(final PluginAccessor delegate)
    {
        this.delegate = delegate;
    }

    public ClassLoader getClassLoader()
    {
        return delegate.getClassLoader();
    }

    public InputStream getDynamicResourceAsStream(final String resourcePath)
    {
        return delegate.getDynamicResourceAsStream(resourcePath);
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz)
    {
        return delegate.getEnabledModuleDescriptorsByClass(descriptorClazz);
    }

    public <M> List<M> getEnabledModulesByClass(final Class<M> moduleClass)
    {
        return delegate.getEnabledModulesByClass(moduleClass);
    }

    public Plugin getEnabledPlugin(final String pluginKey) throws IllegalArgumentException
    {
        return delegate.getEnabledPlugin(pluginKey);
    }

    public ModuleDescriptor<?> getEnabledPluginModule(final String completeKey)
    {
        return delegate.getEnabledPluginModule(completeKey);
    }

    public Collection<Plugin> getEnabledPlugins()
    {
        return delegate.getEnabledPlugins();
    }

    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        return delegate.getModuleDescriptors(moduleDescriptorPredicate);
    }

    public <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
    {
        return delegate.getModules(moduleDescriptorPredicate);
    }

    public Plugin getPlugin(final String key) throws IllegalArgumentException
    {
        return delegate.getPlugin(key);
    }

    public ModuleDescriptor<?> getPluginModule(final String completeKey)
    {
        return delegate.getPluginModule(completeKey);
    }

    public PluginRestartState getPluginRestartState(final String key)
    {
        return delegate.getPluginRestartState(key);
    }

    public Collection<Plugin> getPlugins()
    {
        return delegate.getPlugins();
    }

    public Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate)
    {
        return delegate.getPlugins(pluginPredicate);
    }

    public boolean isPluginEnabled(final String key) throws IllegalArgumentException
    {
        return delegate.isPluginEnabled(key);
    }

    public boolean isPluginModuleEnabled(final String completeKey)
    {
        return delegate.isPluginModuleEnabled(completeKey);
    }

    public boolean isSystemPlugin(final String key)
    {
        return delegate.isSystemPlugin(key);
    }

    //
    // deprecated
    //

    public Class<?> getDynamicPluginClass(final String className) throws ClassNotFoundException
    {
        @SuppressWarnings("deprecation")
        final Class<?> result = delegate.getDynamicPluginClass(className);
        return result;
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz, final boolean verbose)
    {
        @SuppressWarnings("deprecation")
        final List<D> result = delegate.getEnabledModuleDescriptorsByClass(descriptorClazz, verbose);
        return result;
    }

    public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(final String type) throws PluginParseException
    {
        @SuppressWarnings("deprecation")
        final List<ModuleDescriptor<M>> result = delegate.getEnabledModuleDescriptorsByType(type);
        return result;
    }

    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass)
    {
        @SuppressWarnings("deprecation")
        final List<M> result = delegate.getEnabledModulesByClassAndDescriptor(descriptorClass, moduleClass);
        return result;
    }

    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>>[] descriptorClazz, final Class<M> moduleClass)
    {
        @SuppressWarnings("deprecation")
        final List<M> result = delegate.getEnabledModulesByClassAndDescriptor(descriptorClazz, moduleClass);
        return result;
    }

    public InputStream getPluginResourceAsStream(final String pluginKey, final String resourcePath)
    {
        @SuppressWarnings("deprecation")
        final InputStream result = delegate.getPluginResourceAsStream(pluginKey, resourcePath);
        return result;
    }
}
