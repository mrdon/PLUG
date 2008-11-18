package com.atlassian.plugin;

import org.dom4j.Element;

import java.util.Map;

public interface ModuleDescriptor<T> extends Resourced
{
    /**
     * The complete key for this module, including the plugin key.
     * <p>
     * Format is plugin.key:module.key
     * </p>
     */
    String getCompleteKey();

    /**
     * The plugin key for this module, derived from the complete key
     */
    String getPluginKey();

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
    Class<T> getModuleClass();

    /**
     * The particular module object created by this plugin.
     */
    T getModule();

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
     * Whether or not this plugin module is a "system" plugin that shouldn't be made visible/disableable to the user
     * @return
     */
    boolean isSystemModule();

    /**
     * Override this if your plugin needs to clean up when it's been removed.
     * @param plugin
     */
    void destroy(Plugin plugin);

    Float getMinJavaVersion();

    /**
     * If a min java version has been specified this will return true if the running jvm
     * is >= to the specified version. If this is not set then it is treated as not having
     * a preference.
     * @return true if satisfied, false otherwise.
     */
    boolean satisfiesMinJavaVersion();

    Map<String, String> getParams();

    /**
     * Key used to override {@link #getName()} when using internationalisation.
     *
     * @return the i18n key.  May be null.
     */
    String getI18nNameKey();

    /**
     * Key used to override {@link #getDescription()} when using internationalisation.
     *
     * @return the i18n key.  May be null.
     */
    String getDescriptionKey();

    /**
     * @return The plugin this module descriptor is associated with
     */
    Plugin getPlugin();
}
