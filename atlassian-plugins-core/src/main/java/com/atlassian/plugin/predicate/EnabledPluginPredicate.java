package com.atlassian.plugin.predicate;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;

/**
 * A {@link PluginPredicate} that matches enabled plugins.
 */
public class EnabledPluginPredicate implements PluginPredicate
{
    private final PluginAccessor pluginAccessor;

    /**
     * @throws IllegalArgumentException if PluginAccessor is <code>null</code>
     */
    public EnabledPluginPredicate(final PluginAccessor pluginAccessor)
    {
        if (pluginAccessor == null)
        {
            throw new IllegalArgumentException("PluginAccessor must not be null when constructing an EnabledPluginPredicate!");
        }
        this.pluginAccessor = pluginAccessor;
    }

    public boolean matches(final Plugin plugin)
    {
        return plugin != null && pluginAccessor.isPluginEnabled(plugin.getKey());
    }
}
