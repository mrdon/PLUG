package com.atlassian.plugin.loaders;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;

import java.util.Collection;
import java.util.Map;
import java.util.List;

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
     * Removes plugins this PluginLoader can no longer see, returning a list of them.
     * @return List of plugins that were there once but now are not ... Once upon a time
     */
    Collection removeMissingPlugins();

    /**
     * @return a collection of discovered plugins which have now been loaded by this pluginloader
     */
    Collection addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory);

    /**
     * Remove a specific plugin
     */
    void removePlugin(Plugin plugin) throws PluginException;
}
