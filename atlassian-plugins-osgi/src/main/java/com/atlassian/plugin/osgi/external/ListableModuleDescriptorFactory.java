package com.atlassian.plugin.osgi.external;

import com.atlassian.plugin.HostContainerManagedModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;

import java.util.Set;

/**
 * A module descriptor factory that can list its supported module descriptors.
 *
 * @since 2.1.2
 */
public interface ListableModuleDescriptorFactory extends HostContainerManagedModuleDescriptorFactory
{
    Set<Class<ModuleDescriptor<?>>> getModuleDescriptorClasses();
}
