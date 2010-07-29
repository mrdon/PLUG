package com.atlassian.plugin.metadata;

import java.util.Set;

import net.jcip.annotations.Immutable;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.collect.ImmutableSet;

/**
 * Default implementation takes three lots of names and checks if the
 * {@link Plugin#getKey()} or {@link ModuleDescriptor#getCompleteKey()} is in
 * the relevant one.
 * 
 * @since 2.6
 */
@Immutable
final class DefaultPluginMetadata implements PluginMetadata
{
    private final Set<String> pluginKeys;
    private final Set<String> requiredPluginKeys;
    private final Set<String> requiredModules;

    DefaultPluginMetadata(final Iterable<String> pluginKeys, final Iterable<String> requiredPluginKeys, final Iterable<String> requiredModules)
    {
        this.pluginKeys = ImmutableSet.copyOf(pluginKeys);
        this.requiredPluginKeys = ImmutableSet.copyOf(requiredPluginKeys);
        this.requiredModules = ImmutableSet.copyOf(requiredModules);
    }

    public boolean applicationProvided(final Plugin plugin)
    {
        return pluginKeys.contains(plugin.getKey());
    }

    public boolean required(final Plugin plugin)
    {
        return requiredPluginKeys.contains(plugin.getKey());
    }

    public boolean required(final ModuleDescriptor<?> descriptor)
    {
        return requiredModules.contains(descriptor.getCompleteKey());
    }
}