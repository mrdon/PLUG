package com.atlassian.plugin.loaders;

import java.util.Collection;
import java.util.Map;

public interface PluginLoader
{
    Collection getPlugins(Map moduleDescriptors);
}
