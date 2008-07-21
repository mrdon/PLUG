package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.FileUtils;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.zip.UrlUnzipper;
import com.atlassian.plugin.impl.DynamicPlugin;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;

import org.apache.log4j.Logger;

/**
 * Version of the osgi plugin loader that ensures plugins loaded will not be deletable.  Also provides an alternative
 * constructor that will handle unzipping the bundled plugins.
 */
public class BundledOsgiPluginLoader extends OsgiPluginLoader
{
    private static final Logger log = Logger.getLogger(BundledOsgiPluginLoader.class);

    /**
     * Constructs the loader, but also unzips the bundled plugins to the specified plugin directory
     *
     * @param zipUrl The url to the zip of bundled plugins
     * @param pluginPath The directory that should contain the unzipped bundled plugins
     * @param pluginDescriptorFileName The plugin descriptor name, i.e. atlassian-plugins.xml
     * @param pluginFactory The factory to create the plugins
     * @param osgi The osgi container manager
     * @param provider The initial host component provider to use when starting the osgi container (if not already started)
     */
    public BundledOsgiPluginLoader(URL zipUrl, File pluginPath, String pluginDescriptorFileName, PluginFactory pluginFactory, OsgiContainerManager osgi, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory, osgi, provider);
        if (zipUrl == null) 
            throw new IllegalArgumentException("Bundled zip url cannot be null");
        FileUtils.conditionallyExtractZipFile(zipUrl, pluginPath);
    }

    public BundledOsgiPluginLoader(File pluginPath, String pluginDescriptorFileName, PluginFactory pluginFactory, OsgiContainerManager osgi, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory, osgi, provider);
    }

    /**
     * Override the standard plugin loading in order to set isDeleteable = false because we don't ever want to delete
     * a bundled plugin, evern though we do want to dynamically replace them.
     *
     * @param deploymentUnit
     * @param moduleDescriptorFactory
     * @return The new DynamicPlugin with isDeleteable set to false;
     * @throws com.atlassian.plugin.PluginParseException
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

        log.debug("Deploy bundled plugin: "+plugin.getName());

        return plugin;
    }
}
