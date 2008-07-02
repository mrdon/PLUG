package com.atlassian.plugin.impl;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.descriptors.UnloadableModuleDescriptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class to create UnloadablePlugin instances.
 */
public final class UnloadablePluginFactory
{
    /**
     * Creates an UnloadablePlugin instance from a given plugin, when there were problems loading the modules or the plugin itself
     *
     * @param oldPlugin the Plugin that is unloadable
     * @return UnloadablePlugin instance
     */
    public static UnloadablePlugin createUnloadablePlugin(Plugin oldPlugin)
    {
        return createUnloadablePlugin(oldPlugin, null);
    }

    /**
     * Creates an UnloadablePlugin instance from a given plugin.
     *
     * It also allows a problematic ModuleDescriptor to be passed in, which will replace the existing
     * descriptor with the same key in the new plugin.
     *
     * @param oldPlugin the Plugin that is unloadable
     * @param unloadableDescriptor the ModuleDescriptor containing the error
     * @return UnloadablePlugin instance
     */
    public static UnloadablePlugin createUnloadablePlugin(Plugin oldPlugin, UnloadableModuleDescriptor unloadableDescriptor)
    {
        UnloadablePlugin newPlugin = new UnloadablePlugin();

        newPlugin.setName(oldPlugin.getName());
        newPlugin.setKey(oldPlugin.getKey());
        newPlugin.setI18nNameKey(oldPlugin.getI18nNameKey());
        newPlugin.setUninstallable(oldPlugin.isUninstallable());
        newPlugin.setDeletable(oldPlugin.isDeleteable());

        // Make sure it's visible to the user
        newPlugin.setSystemPlugin(false);

        newPlugin.setPluginInformation(oldPlugin.getPluginInformation());

        List moduleDescriptors = new ArrayList(oldPlugin.getModuleDescriptors());
        Iterator descriptorIterator = moduleDescriptors.iterator();

        // Add the existing descriptors to the new plugin
        while (descriptorIterator.hasNext())
        {
            ModuleDescriptor descriptor = (ModuleDescriptor) descriptorIterator.next();

            // If we find the module descriptor that is causing the problem, skip it
            if (unloadableDescriptor != null && descriptor.getKey().equals(unloadableDescriptor.getKey()))
                continue;

            newPlugin.addModuleDescriptor(descriptor);
        }

        // Add the unloadable descriptor to the end (if it exists)
        if (unloadableDescriptor != null)
            newPlugin.addModuleDescriptor(unloadableDescriptor);

        return newPlugin;
    }
}
