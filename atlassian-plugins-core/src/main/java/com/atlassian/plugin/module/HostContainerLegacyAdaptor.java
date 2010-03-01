package com.atlassian.plugin.module;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import org.apache.commons.lang.Validate;

/**
 * Created by IntelliJ IDEA.
 * User: ervzijst
 * Date: Mar 1, 2010
 * Time: 5:01:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostContainerLegacyAdaptor extends LegacyModuleClassFactory
{

    private final HostContainer hostContainer;

    public HostContainerLegacyAdaptor(HostContainer hostContainer)
    {
        Validate.notNull(hostContainer, "hostContainer should not be null");
        this.hostContainer = hostContainer;
    }

    public <T> T createModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException {

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
