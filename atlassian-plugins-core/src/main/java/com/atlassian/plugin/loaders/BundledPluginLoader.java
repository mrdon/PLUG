package com.atlassian.plugin.loaders;

import com.atlassian.plugin.util.FileUtils;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;

import java.io.File;
import java.util.List;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Plugin loader that unzips plugins from a zip file into a local directory, and ensures that directory only contains
 * plugins from that zip file.  It also treats all plugins loaded from the directory as bundled plugins, meaning they
 * can can be upgraded, but not deleted.
 */
public class BundledPluginLoader extends DirectoryPluginLoader {

    private static final Log log = LogFactory.getLog(BundledPluginLoader.class);
    public BundledPluginLoader(URL zipUrl, File pluginPath, List pluginFactories, PluginEventManager eventManager)
    {
        super(pluginPath, pluginFactories, eventManager);
        if (zipUrl == null)
            throw new IllegalArgumentException("Bundled zip url cannot be null");
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
    protected Plugin deployPluginFromUnit(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Plugin plugin = super.deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
        if (plugin instanceof DynamicPlugin)
        {
            DynamicPlugin dplugin = (DynamicPlugin) plugin;
            dplugin.setDeletable(false);
            dplugin.setBundled(true);
        }

        if (log.isDebugEnabled())
            log.debug("Deployed bundled plugin: "+plugin.getName());
        return plugin;
    }
}
