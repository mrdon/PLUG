package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.PluginsClassLoader;

public class DefaultPluginFactory implements PluginFactory
{

    public Plugin createPlugin(DeploymentUnit deploymentUnit, PluginsClassLoader loader)
    {
        return new DynamicPlugin(deploymentUnit, loader);
    }
}
