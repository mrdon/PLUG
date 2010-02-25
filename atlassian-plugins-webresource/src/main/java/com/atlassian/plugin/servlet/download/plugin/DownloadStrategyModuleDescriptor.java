package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
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
    public DownloadStrategyModuleDescriptor(ModuleClassFactory moduleCreator)
    {
        super(moduleCreator);
    }

    public DownloadStrategy getModule()
    {
        return moduleClassFactory.createModuleClass(moduleClassName, this);
    }
}
