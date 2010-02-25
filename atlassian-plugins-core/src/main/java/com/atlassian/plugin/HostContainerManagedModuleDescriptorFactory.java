package com.atlassian.plugin;

import com.atlassian.plugin.hostcontainer.HostContainer;

/**
 * TODO: Document this class / interface here
 */
public interface HostContainerManagedModuleDescriptorFactory extends ModuleDescriptorFactory
{
    HostContainer getHostContainer();

}
