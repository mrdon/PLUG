package com.atlassian.plugin;

/**
 * A plugin manager is responsible for retrieving plugins and modules, as well as managing plugin loading and state.
 *
 * @deprecated since 2006-09-26 the preferred technique is to use the interfaces that this on e extends directly.
 * 
 * @see PluginController
 * @see PluginAccessor
 * @see PluginSystemLifecycle
 */
@Deprecated
public interface PluginManager<T> extends PluginController, PluginAccessor<T>, PluginSystemLifecycle
{
    public static final String PLUGIN_DESCRIPTOR_FILENAME = "atlassian-plugin.xml";
}
