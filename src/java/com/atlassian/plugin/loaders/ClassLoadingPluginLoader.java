package com.atlassian.plugin.loaders;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.Scanner;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.PluginsClassLoader;

import java.util.*;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;

/**
 * A plugin loader to load plugins from a classloading on disk.
 * <p>
 * This creates a classloader for that classloading and loads plugins from all JARs from within it.
 */
public class ClassLoadingPluginLoader extends AbstractXmlPluginLoader
{
    private static Log log = LogFactory.getLog(ClassLoadingPluginLoader.class);
    String fileNameToLoad;
    private Scanner scanner;

    public ClassLoadingPluginLoader(File path)
    {
        this(path, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
    }

    public ClassLoadingPluginLoader(File path, String fileNameToLoad)
    {
        log.debug("Creating classloader for url " + path);
        scanner = new Scanner(path);
        this.fileNameToLoad = fileNameToLoad;
    }

    public Collection getPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        List plugins = new ArrayList();

        scanner.scan();

        for (Iterator iterator = scanner.getDeploymentUnits().iterator(); iterator.hasNext();)
        {
            DeploymentUnit deploymentUnit = (DeploymentUnit) iterator.next();
            PluginsClassLoader loader = (PluginsClassLoader)scanner.getClassLoader(deploymentUnit);

            URL pluginDescriptor = loader.getResource(fileNameToLoad);

            if (pluginDescriptor == null)
            {
                log.error("No descriptor found in classloader for : " + deploymentUnit);
            }
            else
            {
                DynamicPlugin plugin = new DynamicPlugin(deploymentUnit, loader);
                InputStream is = loader.getResourceAsStream(fileNameToLoad);

                try
                {
                    configurePlugin(moduleDescriptorFactory, getDocument(is), plugin);
                    plugins.add(plugin);
                }
                catch (DocumentException e)
                {
                    log.error("Error getting descriptor document for : " + deploymentUnit, e);
                }
                catch (PluginParseException e)
                {
                    log.error("Error loading descriptor for : " + deploymentUnit, e);
                }
                finally
                {
                    try
                    {
                        is.close();
                    }
                    catch (IOException e)
                    {
                        log.error("Error closing input stream for descriptor in: " + deploymentUnit, e);
                    }
                }
            }
        }

        return plugins;
    }
}
