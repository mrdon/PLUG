package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.PluginsClassLoader;

public interface PluginFactory
{

    Plugin createPlugin(DeploymentUnit deploymentUnit, PluginsClassLoader loader);
}
