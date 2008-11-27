package com.atlassian.plugin.factories;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

/**
 * Creates the plugin artifact and deploys it into the appropriate plugin management system
 * @since 2.0.0
 */
public interface PluginFactory
{
    /**
     * Determines if this factory can handle this artifact.
     *
     * @param pluginArtifact The artifact to test
     * @return The plugin key, null if it cannot load the plugin
     * @throws com.atlassian.plugin.PluginParseException If there are exceptions parsing the plugin configuration when
     * the deployer should have been able to deploy the plugin
     */
    String canCreate(PluginArtifact pluginArtifact) throws PluginParseException;

    /**
     * Deploys the deployment unit by instantiating the plugin and configuring it.  Should only be called if the respective
     * {@link #canCreate(PluginArtifact)} call returned the plugin key
     *
     * @param deploymentUnit the unit to deploy
     * @param moduleDescriptorFactory the factory for the module descriptors
     * @return the plugin loaded from the deployment unit, or an UnloadablePlugin instance if loading fails.
     * @throws com.atlassian.plugin.PluginParseException if the plugin could not be parsed
     */
    <T> Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory<T, ModuleDescriptor<? extends T>> moduleDescriptorFactory) throws PluginParseException;
}
