package com.atlassian.plugin;

import java.util.List;
import java.util.Collection;

/**
 * A plugin manager is responsible for retrieving plugins and modules, as well as managing plugin loading and state.
 */
public interface PluginManager
{
    public static final String PLUGIN_DESCRIPTOR_FILENAME = "atlassian-plugin.xml";

    /**
     * Get all enabled modules that have a specific descriptor class.
     */
    public List getEnabledModulesByDescriptor(Class descriptorClazz);

    void init() throws PluginParseException;

    Collection getPlugins();

    Plugin getPlugin(String key);

    ModuleDescriptor getPluginModule(String completeKey);

    void enableLibrary(String key);

    boolean isPluginEnabled(String key);

    Collection getPluginModule(Class moduleClass);
}
