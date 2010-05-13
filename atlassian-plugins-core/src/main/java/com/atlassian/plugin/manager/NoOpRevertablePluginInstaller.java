package com.atlassian.plugin.manager;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginInstaller;
import com.atlassian.plugin.RevertablePluginInstaller;

/**
 * Wraps a plugin installer as a {@link com.atlassian.plugin.RevertablePluginInstaller} that does nothing
 * in its implementation.
 *
 * @since 2.5.0
 */
class NoOpRevertablePluginInstaller implements RevertablePluginInstaller
{
    private final PluginInstaller delegate;

    public NoOpRevertablePluginInstaller(PluginInstaller delegate)
    {
        this.delegate = delegate;
    }

    public void revertInstalledPlugin(String pluginKey)
    {
        // op-op
    }

    public void clearBackups()
    {
        // no-op
    }

    public void installPlugin(String key, PluginArtifact pluginArtifact)
    {
        delegate.installPlugin(key, pluginArtifact);
    }
}
