package com.atlassian.plugin;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public interface Plugin extends Resourced, Comparable
{
    public static final Comparator NAME_COMPARATOR = new PluginNameComparator();

    int getPluginsVersion();

    void setPluginsVersion(int version);

    String getName();

    void setName(String name);

    String getI18nNameKey();

    void setI18nNameKey(String i18nNameKey);

    String getKey();

    void setKey(String aPackage);

    void addModuleDescriptor(ModuleDescriptor moduleDescriptor);

    Collection getModuleDescriptors();

    ModuleDescriptor getModuleDescriptor(String key);

    List getModuleDescriptorsByModuleClass(Class aClass);

    boolean isEnabledByDefault();

    void setEnabledByDefault(boolean enabledByDefault);

    PluginInformation getPluginInformation();

    void setPluginInformation(PluginInformation pluginInformation);

    void setResources(Resourced resources);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Whether the plugin is a "system" plugin that shouldn't be made visible to the user
     */
    boolean isSystemPlugin();

    boolean containsSystemModule();

    void setSystemPlugin(boolean system);

    /**
     * Whether the plugin is a "bundled" plugin that can't be removed.
     */
    boolean isBundledPlugin();

    /**
     * The date this plugin was loaded into the system.
     */
    Date getDateLoaded();

    /**
     * Whether or not this plugin can be 'uninstalled'.
     */
    boolean isUninstallable();

    /**
     * Should the plugin file be deleted on unistall?
     */
    boolean isDeleteable();

    /**
     * Whether or not this plugin is loaded dynamically at runtime
     */
    boolean isDynamicallyLoaded();

    /**
     * Get the plugin to load a specific class.
     *
     * @param clazz        The name of the class to be loaded
     * @param callingClass The class calling the loading (used to help find a classloader)
     * @return The loaded class.
     * @throws ClassNotFoundException
     */
    Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException;

    /**
     * Get the classloader for the plugin.
     * 
     * @return The classloader used to load classes for this plugin
     */
    ClassLoader getClassLoader();
    
    /**
     * Retrieve the URL of the resource from the plugin.
     * 
     * @param path the name of the resource to be loaded
     * @return The URL to the resource, or null if the resource is not found
     */
    URL getResource(String path);

    /**
     * Load a given resource from the plugin. Plugins that are loaded dynamically will need
     * to implement this in a way that loads the resource from the same context as the plugin.
     * Static plugins can just pull them from their own classloader.
     *
     * @param name The name of the resource to be loaded.
     * @return An InputStream for the resource, or null if the resource is not found.
     */
    InputStream getResourceAsStream(String name);

    /**
     * Free any resources held by this plugin.  To be called during uninstallation of the {@link Plugin}.
     */
    void close();
}
