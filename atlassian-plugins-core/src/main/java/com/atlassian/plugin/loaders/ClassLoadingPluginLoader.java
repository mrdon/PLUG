package com.atlassian.plugin.loaders;

import java.io.File;

/**
 * @deprecated Since 2.0.0, use {@link DirectoryPluginLoader} instead
 */
public class ClassLoadingPluginLoader extends DirectoryPluginLoader
{

    public ClassLoadingPluginLoader(File path, PluginFactory pluginFactory) {
        super(path, pluginFactory);
    }

    public ClassLoadingPluginLoader(File path, String pluginDescriptorFileName, PluginFactory pluginFactory) {
        super(path, pluginDescriptorFileName, pluginFactory);
    }
}
