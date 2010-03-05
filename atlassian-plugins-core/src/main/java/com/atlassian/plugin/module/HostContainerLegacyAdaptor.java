package com.atlassian.plugin.module;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import org.apache.commons.lang.Validate;

/**
 * Legacy module factory that uses the deprecated {@link AutowireCapablePlugin} interface
 *
 * @since 2.5.0
 */
public class HostContainerLegacyAdaptor extends LegacyModuleFactory
{

    private final HostContainer hostContainer;

    public HostContainerLegacyAdaptor(HostContainer hostContainer)
    {
        Validate.notNull(hostContainer, "hostContainer should not be null");
        this.hostContainer = hostContainer;
    }

    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException {

        // Give the plugin a go first
        if (moduleDescriptor.getPlugin() instanceof AutowireCapablePlugin)
        {
            return ((AutowireCapablePlugin) moduleDescriptor.getPlugin()).autowire(moduleDescriptor.getModuleClass());
        }
        else
        {
            return hostContainer.create(moduleDescriptor.getModuleClass());
        }
    }

}
