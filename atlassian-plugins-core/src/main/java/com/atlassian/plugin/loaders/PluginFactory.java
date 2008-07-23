package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

/**
 * @deprecated Since 2.0.0, use a custom {@link com.atlassian.plugin.loaders.deployer.PluginDeployer} instead
 */
public interface PluginFactory
{
    Plugin createPlugin(DeploymentUnit deploymentUnit, PluginClassLoader loader);
}
