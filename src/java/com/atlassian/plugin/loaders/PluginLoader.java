package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginParseException;

import java.util.Collection;
import java.util.Map;

public interface PluginLoader
{
    Collection getPlugins(Map moduleDescriptors) throws PluginParseException;
}
