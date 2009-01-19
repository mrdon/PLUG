package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.AutowireCapablePlugin;

/**
 * A plugin module which provides a {@link DownloadStrategy}.
 *
 * @see DownloadStrategy
 * @see PluggableDownloadStrategy
 * @since 2.2.0
 */
public class DownloadStrategyModuleDescriptor extends AbstractModuleDescriptor<DownloadStrategy>
{
    private final HostContainer hostContainer;

    public DownloadStrategyModuleDescriptor(HostContainer hostContainer)
    {
        this.hostContainer = hostContainer;
    }

    public DownloadStrategy getModule()
    {
        if (plugin instanceof AutowireCapablePlugin)
        {
            return ((AutowireCapablePlugin) plugin).autowire(getModuleClass());
        }
        return hostContainer.create(getModuleClass());
    }
}
