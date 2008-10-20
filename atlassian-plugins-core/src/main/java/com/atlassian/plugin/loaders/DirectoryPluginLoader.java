package com.atlassian.plugin.loaders;

import com.atlassian.plugin.*;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.loaders.classloading.Scanner;
import com.atlassian.plugin.loaders.DirectoryScanner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;

import java.io.File;
import java.util.*;
import java.net.MalformedURLException;

/**
 * A plugin loader to load plugins from a directory on disk.  A {@link DirectoryScanner} is used to locate plugin artifacts
 * and determine if they need to be redeployed or not.
 */
public class DirectoryPluginLoader extends ScanningPluginLoader
{

    /**
     * Constructs a loader for a particular directory and set of deployers
     * @param path The directory containing the plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public DirectoryPluginLoader(File path, List<PluginFactory> pluginFactories, PluginEventManager pluginEventManager)
    {
        super(new DirectoryScanner(path), pluginFactories, pluginEventManager);
    }

    /**
     * Constructs a loader for a particular directory and set of deployers
     * @param path The directory containing the plugins
     * @param pluginFactories The deployers that will handle turning an artifact into a plugin
     * @param pluginArtifactFactory The plugin artifact factory
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.1.0
     */
    public DirectoryPluginLoader(File path, List<PluginFactory> pluginFactories, PluginArtifactFactory pluginArtifactFactory,
                                 PluginEventManager pluginEventManager)
    {
        super(new DirectoryScanner(path), pluginFactories, pluginArtifactFactory, pluginEventManager);
    }
}
