package com.atlassian.plugin;

/**
 * A plugin manager is responsible for retrieving plugins and modules, as well as managing plugin loading and state.
 *
 * @deprecated since 2006-09-26
 * @see PluginController
 * @see PluginAccessor
 */
public interface PluginManager extends PluginController, PluginAccessor
{
    public static final String PLUGIN_DESCRIPTOR_FILENAME = "atlassian-plugin.xml";

    /**
     * Initialise the plugin manager. This <b>must</b> be called before anything else.
     * @throws PluginParseException If parsing the plugins failed.
     */
    void init() throws PluginParseException;

}
