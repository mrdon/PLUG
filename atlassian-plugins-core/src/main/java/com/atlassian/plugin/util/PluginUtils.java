package com.atlassian.plugin.util;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.RequiresRestart;

import java.util.Set;

import org.dom4j.Element;
import org.apache.commons.lang.Validate;

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

    /**
     * Determines if a module element applies to the current application by matching the 'application' attribute
     * to the set of keys.  If the application is specified, but isn't in the set, we return false
     * @param element The module element
     * @param keys The set of application keys
     * @return True if it should apply, false otherwise
     * @since 2.2.0
     */
    public static boolean doesModuleElementApplyToApplication(Element element, Set<String> keys)
    {
        Validate.notNull(keys);
        Validate.notNull(element);
        String key = element.attributeValue("application");
        return !(key != null && !keys.contains(key));
    }
}
