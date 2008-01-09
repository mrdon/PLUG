package com.atlassian.plugin;

import com.atlassian.plugin.predicate.ModulePredicate;
import com.atlassian.plugin.predicate.PluginPredicate;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Allows access to the current plugin system state
 */
public interface PluginAccessor
{
    /**
     * Gets all of the currently installed plugins.
     * @return a collection of installed {@link Plugin}s.
     */
    Collection getPlugins();

    /**
     * Gets all installed plugins that match the given predicate.
     * @param pluginPredicate the {@link PluginPredicate} to match.
     * @return a collection of {@link Plugin}s that match the given predicate.
     * @since 0.17
     */
    Collection getPlugins(final PluginPredicate pluginPredicate);

    /**
     * Gets all installed modules that match the given predicate.
     * @param modulePredicate the {@link ModulePredicate} to match.
     * @return a collection of modules as per {@link ModuleDescriptor#getModule()} that match the given predicate.
     * @since 0.17
     */
    Collection getModules(final ModulePredicate modulePredicate);

    /**
     * Gets all module descriptors of installed modules that match the given predicate.
     * @param modulePredicate the {@link ModulePredicate} to match.
     * @return a collection of {@link ModuleDescriptor}s that match the given predicate.
     * @since 0.17
     */
    Collection getModuleDescritpors(final ModulePredicate modulePredicate);

    /**
     * Get all of the currently enabled plugins.
     * @return a collection of installed and enabled {@link Plugin}s.
     * @deprecated since 0.17, use {@link #getPlugins(PluginPredicate)} with {@link com.atlassian.plugin.predicate.EnabledPluginPredicate} instead.
     */
    Collection getEnabledPlugins();

    /**
     * Retrieve a given plugin (whether enabled or not).
     * @return The enabled plugin, or null if that plugin does not exist.
     */
    Plugin getPlugin(String key);

    /**
     * Retrieve a given plugin if it is enabled.
     * @return The enabled plugin, or null if that plugin does not exist or is disabled.
     */
    Plugin getEnabledPlugin(String pluginKey);

    /**
     * Retrieve any plugin module by complete module key.
     * <p>
     * Note: the module may or may not be disabled.
     */
    ModuleDescriptor getPluginModule(String completeKey);

    /**
     * Retrieve an enabled plugin module by complete module key.
     */
    ModuleDescriptor getEnabledPluginModule(String completeKey);

    /**
     * Whether or not a given plugin is currently enabled.
     */
    boolean isPluginEnabled(String key);

    /**
     * Whether or not a given plugin module is currently enabled.  This also checks
     * if the plugin it is contained within is enabled also
     * @see #isPluginEnabled(String)
     */
    boolean isPluginModuleEnabled(String completeKey);

    /**
     * Retrieve all plugin modules that implement or extend a specific class.
     *
     * @return List of modules that implement or extend the given class.
     * @deprecated since 0.17, use {@link #getModules(ModulePredicate)} with an appropriate predicate instead.
     */
    List getEnabledModulesByClass(Class moduleClass);

    /**
     * Retrieve all plugin modules that implement or extend a specific class, and has a descriptor class
     * as one of descriptorClazz
     *
     * @param descriptorClazz @NotNull
     * @param moduleClass @NotNull
     * @return List of modules that implement or extend the given class. Empty list if none found
     * @deprecated since 0.17, use {@link #getModules(ModulePredicate)} with an appropriate predicate instead.
     */
    List getEnabledModulesByClassAndDescriptor(Class[] descriptorClazz, Class moduleClass);

    /**
     * Retrieve all plugin modules that implement or extend a specific class, and has a descriptor class
     * as the descriptorClazz
     *
     * @param descriptorClazz @NotNull
     * @param moduleClass @NotNull
     * @return List of modules that implement or extend the given class. Empty list if none found
     * @deprecated since 0.17, use {@link #getModules(ModulePredicate)} with an appropriate predicate instead.
     */
    List getEnabledModulesByClassAndDescriptor(Class descriptorClazz, Class moduleClass);

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     * @deprecated since 0.17, use {@link #getModuleDescritpors(ModulePredicate)} with an appropriate predicate instead.
     */
    List getEnabledModuleDescriptorsByClass(Class descriptorClazz);

    /**
     * Get all enabled module descriptors that have a specific descriptor type.
     *
     * @return List of {@link ModuleDescriptor}s that are of a given type.
     * @deprecated since 0.17, use {@link #getModuleDescritpors(ModulePredicate)} with an appropriate predicate instead.
     */
    List getEnabledModuleDescriptorsByType(String type) throws PluginParseException;

    /**
     * Retrieve a resource from a currently loaded (and active) dynamically loaded plugin. Will return the first resource
     * found, so plugins with overlapping resource names will behave eratically.
     *
     * @param resourcePath the path to the resource to retrieve
     * @return the dynamically loaded resource that matches that path, or null if no such resource is found
     */
    InputStream getDynamicResourceAsStream(String resourcePath);

    /**
     * Retrieve a resource from a currently loaded (and active) plugin. For statically loaded plugins, this just means
     * pulling the resource from the PluginManager's classloader. For dynamically loaded plugins, this means retrieving
     * the resource from the plugin's private classloader.
     */
    InputStream getPluginResourceAsStream(String pluginKey, String resourcePath);

    /**
     * @return true if the plugin is a system plugin.
     */
    boolean isSystemPlugin(String key);
}
