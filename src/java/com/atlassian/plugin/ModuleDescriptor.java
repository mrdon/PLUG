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

    String getName();

    Class getModuleClass();

    Object getModule();

    String getDescription();

    /**
     * Initialise a module given it's parent plugin and the XML element representing the module.
     */ 
    void init(Plugin plugin, Element element) throws PluginParseException;
}
