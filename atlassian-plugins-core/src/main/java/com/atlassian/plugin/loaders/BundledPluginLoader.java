package com.atlassian.plugin.loaders;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.util.FileUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Plugin loader that unzips plugins from a zip file into a local directory, and ensures that directory only contains
 * plugins from that zip file.  It also treats all plugins loaded from the directory as bundled plugins, meaning they
 * can can be upgraded, but not deleted.
 */
public class BundledPluginLoader<T> extends DirectoryPluginLoader<T>
{

    private static final Log log = LogFactory.getLog(BundledPluginLoader.class);

    public BundledPluginLoader(final URL zipUrl, final File pluginPath, final List<PluginFactory> pluginFactories, final PluginEventManager eventManager)
    {
        super(pluginPath, pluginFactories, eventManager);
        if (zipUrl == null)
        {
            throw new IllegalArgumentException("Bundled zip url cannot be null");
        }
        FileUtils.conditionallyExtractZipFile(zipUrl, pluginPath);
    }

    /**
     * Just like the {@link DirectoryPluginLoader#deployPluginFromUnit(DeploymentUnit,ModuleDescriptorFactory)} method
     * but changes the plugin to not be deletable
     * @param deploymentUnit The plugin to deploy
     * @param moduleDescriptorFactory The descriptor factory
     * @return The created plugin
     * @throws PluginParseException If there is a problem parsing the configuration
     */
    @Override
    protected Plugin deployPluginFromUnit(final DeploymentUnit deploymentUnit, final ModuleDescriptorFactory<T, ModuleDescriptor<? extends T>> moduleDescriptorFactory) throws PluginParseException
    {
        final Plugin plugin = super.deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
        if (plugin instanceof DynamicPlugin)
        {
            final DynamicPlugin dplugin = (DynamicPlugin) plugin;
            dplugin.setDeletable(false);
            dplugin.setBundled(true);
        }

        if (log.isDebugEnabled())
        {
            log.debug("Deployed bundled plugin: " + plugin.getName());
        }
        return plugin;
    }
}
