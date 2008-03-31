package com.atlassian.plugin.parsers;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;

/**
 * Interface for parsing a plugin descriptor file, e.g. atlassian-plugin.xml.
 *
 * @see XmlDescriptorParser
 * @see DescriptorParserFactory
 */
public interface DescriptorParser
{
    /**
     * Sets the configuration on the plugin argument to match the configuration specified in the
     * plugin descriptor (typically an XML file).
     *
     * @param moduleDescriptorFactory a factory for instantiating the required plugin modules
     * @param plugin the plugin whose configuration will be modified
     * @return the original plugin with the configuration changed and the module descriptors added
     * @throws PluginParseException if there was an error getting information about the plugin
     */
    Plugin configurePlugin(ModuleDescriptorFactory moduleDescriptorFactory, Plugin plugin) throws PluginParseException;

    /**
     * @return the key of the plugin specified in the descriptor
     */
    String getKey();

    /**
     * @return true if this plugin is marked as a system plugin in the descriptor. This should only be
     * acted on by plugin loaders which can trust their plugins implicitly (e.g. a classpath plugin
     * loader).
     */
    boolean isSystemPlugin();
}
