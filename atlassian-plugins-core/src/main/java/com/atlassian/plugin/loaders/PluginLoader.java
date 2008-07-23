package com.atlassian.plugin.loaders;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;

import java.util.Collection;

/**
 * Handles loading and unloading plugin artifacts from a location
 */
public interface PluginLoader
{
    Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    /**
     * @return true if this PluginLoader tracks whether or not plugins are added to it.
     */
    boolean supportsAddition();

    /**
     * @return true if this PluginLoader tracks whether or not plugins are removed from it.
     */
    boolean supportsRemoval();

    /**
     * @return a collection of discovered plugins which have now been loaded by this pluginloader
     */
    Collection addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    /**
     * Remove a specific plugin
     */
    void removePlugin(Plugin plugin) throws PluginException;

}
