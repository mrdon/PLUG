package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;

interface PluginMetadata
{
    boolean applicationProvided(Plugin plugin);

    boolean required(Plugin plugin);

    boolean required(ModuleDescriptor<?> descriptor);
}