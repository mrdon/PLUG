package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;

import java.util.Collection;
import java.util.Map;

public interface PluginLoader
{
    Collection getPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;
}
