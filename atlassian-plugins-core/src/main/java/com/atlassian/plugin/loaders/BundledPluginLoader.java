package com.atlassian.plugin.loaders;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Plugin loader that unzips plugins from a zip file into a local directory, and ensures that directory only contains
 * plugins from that zip file.  It also treats all plugins loaded from the directory as bundled plugins, meaning they
 * can can be upgraded, but not deleted.
 */
public class BundledPluginLoader extends DirectoryPluginLoader
{
    public BundledPluginLoader(final URL zipUrl, final File pluginPath, final List<PluginFactory> pluginFactories, final PluginEventManager eventManager)
    {
        super(pluginPath, pluginFactories, eventManager);
        if (zipUrl == null)
        {
            throw new IllegalArgumentException("Bundled zip url cannot be null");
        }
        FileUtils.conditionallyExtractZipFile(zipUrl, pluginPath);
    }

    @Override
    protected void postProcess(final Plugin plugin)
    {
        if (plugin instanceof DynamicPlugin)
        {
            final DynamicPlugin dplugin = (DynamicPlugin) plugin;
            dplugin.setDeletable(false);
            dplugin.setBundled(true);
        }
    }
}
