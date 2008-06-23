package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

public interface PluginFactory
{
    Plugin createPlugin(DeploymentUnit deploymentUnit, PluginClassLoader loader);
}
