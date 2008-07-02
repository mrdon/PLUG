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
     * @param zipUrlPath The path to the zip of bundled plugins
     * @param pluginPath The directory that should contain the unzipped bundled plugins
     * @param pluginDescriptorFileName The plugin descriptor name, i.e. atlassian-plugins.xml
     * @param pluginFactory The factory to create the plugins
     * @param osgi The osgi container manager
     * @param provider The initial host component provider to use when starting the osgi container (if not already started)
     */
    public BundledOsgiPluginLoader(String zipUrlPath, File pluginPath, String pluginDescriptorFileName, PluginFactory pluginFactory, OsgiContainerManager osgi, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory, osgi, provider);
        extractBundledPlugins(zipUrlPath, pluginPath);
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
        DynamicPlugin plugin = (DynamicPlugin) super.deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
        plugin.setDeletable(false);
        plugin.setBundled(true);

        log.debug("Deploy bundled plugin: "+plugin.getName());

        return plugin;
    }

    private void extractBundledPlugins(String zipUrlPath, File destDir)
    {
        // Look for bundled plugins jar
        URL zipUrl = ClassLoaderUtils.getResource(zipUrlPath, BundledOsgiPluginLoader.class);
        if (zipUrl == null)
        {
            log.error("Couldn't find " + zipUrlPath + " on classpath");
            return;
        }

        try
        {

            UrlUnzipper unzipper = new UrlUnzipper(zipUrl, destDir);

            List zipContents = new ArrayList();

            ZipEntry[] zipEntries = unzipper.entries();
            for (int i = 0; i < zipEntries.length; i++)
            {
                zipContents.add(zipEntries[i].getName());
            }

            // If the jar contents of the directory does not match the contents of the zip
            // The we will nuke the bundled plugins directory and re-extract.
            List bundledPluginContents = getContentsOfBundledPluginsDir(destDir);
            if (!bundledPluginContents.equals(zipContents))
            {
                FileUtils.deleteDir(destDir);
                unzipper.unzip();
            }
            else
            {
                if (log.isDebugEnabled())
                    log.debug("Bundled Plugins directory contents match Bundled Plugins zip contents. Do nothing.");
            }
        }
        catch (IOException e)
        {
            log.error("Found " + zipUrlPath + ", but failed to read file", e);
        }
    }

    protected List getContentsOfBundledPluginsDir(File dir)
    {
        // Create filter that lists only jars
        FilenameFilter filter = new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".jar");
            }
        };

        String[] children = dir.list(filter);

        if (children == null)
        {
            // No files, return empty array
            return Collections.EMPTY_LIST;
        }

        ArrayList bundledPluginContents = new ArrayList();

        if (log.isDebugEnabled() && children.length > 0)
            log.debug("Listing JAR files in " + dir.getAbsolutePath());

        for (int i = 0; i < children.length; i++)
        {
            if (log.isDebugEnabled())
                log.debug(children[i]);
            bundledPluginContents.add(children[i]);

        }
        return bundledPluginContents;
    }
}
