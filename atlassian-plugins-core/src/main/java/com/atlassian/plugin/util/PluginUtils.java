package com.atlassian.plugin.util;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.descriptors.RequiresRestart;

/**
 * General plugin utility methods
 */
public class PluginUtils
{
    /**
     * Determines if a plugin requires a restart after being installed at runtime.  Looks for the annotation
     * {@link RequiresRestart} on the plugin's module descriptors.
     *
     * @param plugin The plugin that was just installed at runtime, but not yet enabled
     * @return True if a restart is required
     */
    public static boolean doesPluginRequireRestart(Plugin plugin)
    {
        boolean requiresRestart = false;
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors())
        {
            if (descriptor.getClass().getAnnotation(RequiresRestart.class) != null)
            {
                requiresRestart = true;
                break;
            }
        }
        return requiresRestart;
    }
}
