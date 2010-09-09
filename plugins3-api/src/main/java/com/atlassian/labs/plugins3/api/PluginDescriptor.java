package com.atlassian.labs.plugins3.api;

import java.net.URISyntaxException;

/**
 *
 */
public interface PluginDescriptor
{
    void config(ApplicationInfo context, PluginDescriptorGenerator configurator) throws Exception;
}
