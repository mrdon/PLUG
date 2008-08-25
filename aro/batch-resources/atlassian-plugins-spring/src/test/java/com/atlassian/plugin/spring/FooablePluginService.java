package com.atlassian.plugin.spring;

import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;

@AvailableToPlugins(contextClassLoaderStrategy = ContextClassLoaderStrategy.USE_PLUGIN)
public class FooablePluginService implements Fooable
{
    public void sayHi()
    {
    }
}
