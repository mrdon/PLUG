package com.atlassian.labs.plugins3.spi;

import com.atlassian.labs.plugins3.api.ApplicationInfo;
import com.atlassian.labs.plugins3.api.PluginDescriptorGenerator;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import org.dom4j.Document;

/**
 *
 */
public interface PluginDescriptorGeneratorFactory
{
    PluginDescriptorGenerator newInstance(Document doc);

    ApplicationInfo getApplicationInfo();

    ModuleDescriptorFactory getModuleDescriptorFactory(Plugin plugin);
}
