package com.atlassian.plugin.metadata;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.google.common.base.Supplier;

/**
 * Describes whether plugins are required or provided by the host application.
 * 
 * @since 2.6
 */
interface PluginMetadata
{
    /**
     * Is the {@link Plugin} provided by (bundled with) the application.
     * 
     * @param plugin the plugin
     * @return true if the application bundled the plugin.
     */
    boolean applicationProvided(Plugin plugin);

    /**
     * Is the {@link Plugin} required by the application for basic operation.
     * 
     * @param plugin the plugin
     * @return true if the application requires the plugin.
     */
    boolean required(Plugin plugin);

    /**
     * Is the {@link ModuleDescriptor} required by the application for basic
     * operation.
     * 
     * @param descriptor the module descriptor
     * @return true if the application requires the module descriptor.
     */
    boolean required(ModuleDescriptor<?> descriptor);

}