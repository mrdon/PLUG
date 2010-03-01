package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.HostContainerLegacyAdaptor;
import com.atlassian.plugin.module.ModuleClassFactory;
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
     * @deprecated  Since 2.5.0, use {@link #DownloadStrategyModuleDescriptor(com.atlassian.plugin.module.ModuleClassFactory)} instead.
     * @param hostContainer
     */
    public DownloadStrategyModuleDescriptor(HostContainer hostContainer)
    {
        this (new HostContainerLegacyAdaptor(hostContainer));
    }

    public DownloadStrategyModuleDescriptor(ModuleClassFactory moduleCreator)
    {
        super(moduleCreator);
    }

    public DownloadStrategy getModule()
    {
        return moduleClassFactory.createModuleClass(moduleClassName, this);
    }
}
