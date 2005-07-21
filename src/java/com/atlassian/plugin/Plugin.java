package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.Collection;
import java.util.List;
import java.io.InputStream;

public interface Plugin extends Resourced
{
    String getName();

    void setName(String name);

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

    /**
     * Whether the plugin is a "system" plugin that shouldn't be made visible to the user
     */
    boolean isSystemPlugin();

    void setSystemPlugin(boolean system);

    void setEnabled(boolean enabled);

    /**
     * Whether or not this plugin can be 'uninstalled'.
     */
    boolean isUninstallable();

    /**
     * Whether or not this plugin is loaded dynamically at runtime
     */
    boolean isDynamicallyLoaded();

    /**
     * Get the plugin to load a specific class.
     *
     * @param clazz The name of the class to be loaded
     * @param callingClass The class calling the loading (used to help find a classloader)
     * @return The loaded class.
     * @throws ClassNotFoundException
     */
    Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException;

    /**
     * Load a given resource from the plugin. Plugins that are loaded dynamically will need
     * to implement this in a way that loads the resource from the same context as the plugin.
     * Static plugins can just pull them from their own classloader.
     *
     * @param name The name of the resource to be loaded.
     * @return An InputStream for the resource, or null if the resource is not found.
     */
    InputStream getResourceAsStream(String name);
}
