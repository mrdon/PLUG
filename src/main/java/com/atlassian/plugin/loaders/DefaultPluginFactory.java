package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

public class DefaultPluginFactory implements PluginFactory
{
    public Plugin createPlugin(DeploymentUnit deploymentUnit, PluginClassLoader loader)
    {
        return new DynamicPlugin(deploymentUnit, loader);
    }
}
