package com.atlassian.plugin;

import org.dom4j.Element;

public interface ModuleDescriptor
{
    /**
     * The complete key for this module, including the plugin key.
     * <p>
     * Format is plugin.key:module.key
     * </p>
     */
    String getCompleteKey();

    /**
     * The key for this module, unique within the plugin.
     */
    String getKey();

    /**
     * A simple string name for this descriptor.
     */
    String getName();

    /**
     * A simple description of this descriptor.
     */ 
    String getDescription();

    /**
     * The class of the module this descriptor creates.
     */
    Class getModuleClass();

    /**
     * The particular module object created by this plugin.
     */
    Object getModule();


    /**
     * Initialise a module given it's parent plugin and the XML element representing the module.
     */ 
    void init(Plugin plugin, Element element) throws PluginParseException;

    /**
     * Whether or not this plugin module is enabled by default.
     * @return
     */
    boolean isEnabledByDefault();

    /**
     * Override this if your plugin needs to clean up when it's been removed.
     * @param plugin
     */
    void destroy(Plugin plugin);
}
