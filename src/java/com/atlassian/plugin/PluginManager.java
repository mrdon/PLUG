package com.atlassian.plugin;

import java.util.List;
import java.util.Collection;

/**
 * A plugin manager is responsible for retrieving plugins and modules, as well as managing plugin loading and state.
 */
public interface PluginManager
{
    public static final String PLUGIN_DESCRIPTOR_FILENAME = "atlassian-plugin.xml";

    void init() throws PluginParseException;

    Collection getPlugins();

    Plugin getPlugin(String key);

    ModuleDescriptor getPluginModule(String completeKey);

    void enablePlugin(String key);

    void disablePlugin(String key);

    boolean isPluginEnabled(String key);

    /**
     * Retrieve all plugin modules that implement or extend a specific class.
     *
     * @return List of modules that implement or extend the given class.
     */
    Collection getEnabledModulesByClass(Class moduleClass);

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     */
    List getEnabledModuleDescriptorsByClass(Class descriptorClazz);

}
