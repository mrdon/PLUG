package com.atlassian.plugin.predicate;

import com.atlassian.plugin.Plugin;

/**
 * Interface used to match plugins according to implementation specific rules.
 *
 * @since 0.17
 */
public interface PluginPredicate
{
    /**
     * <p>Will match a plugin according to implementation rules.<p>
     * <p>This method must not change the state of the plugin.</p>
     *
     * @param plugin the plugin to test against.
     * @return <code>true</code> if the plugin matches the predicate, <code>false</code> otherwise.
     */
    boolean matches(final Plugin plugin);
}
