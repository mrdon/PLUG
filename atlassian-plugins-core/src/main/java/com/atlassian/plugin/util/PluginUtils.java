package com.atlassian.plugin.util;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.RequiresRestart;

/**
 * General plugin utility methods
 *
 * @since 2.1
 */
public class PluginUtils
{
    /**
     * Determines if a plugin requires a restart after being installed at runtime.  Looks for the annotation
     * {@link RequiresRestart} on the plugin's module descriptors.
     *
     * @param plugin The plugin that was just installed at runtime, but not yet enabled
     * @return True if a restart is required
     * @since 2.1
     */
    public static boolean doesPluginRequireRestart(final Plugin plugin)
    {
        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors())
        {
            if (descriptor.getClass().getAnnotation(RequiresRestart.class) != null)
            {
                return true;
            }
        }
        return false;
    }
}
