package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.HostContainerLegacyAdaptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.servlet.DownloadStrategy;

/**
 * A plugin module which provides a {@link DownloadStrategy}.
 *
 * @see DownloadStrategy
 * @see PluggableDownloadStrategy
 * @since 2.2.0
 */
public class DownloadStrategyModuleDescriptor extends AbstractModuleDescriptor<DownloadStrategy>
{
    /**
     * Creates a download strategy.
     * @param moduleCreator The factory to create module instances
     * @Since 2.5.0
     */
    public DownloadStrategyModuleDescriptor(ModuleFactory moduleCreator)
    {
        super(moduleCreator);
    }

    public DownloadStrategy getModule()
    {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
