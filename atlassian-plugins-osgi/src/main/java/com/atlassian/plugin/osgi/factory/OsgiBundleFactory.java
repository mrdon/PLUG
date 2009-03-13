package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Plugin deployer that deploys OSGi bundles that don't contain XML descriptor files
 */
public class OsgiBundleFactory implements PluginFactory
{
    private static final Log log = LogFactory.getLog(OsgiBundleFactory.class);

    private final OsgiContainerManager osgi;
    private final PluginEventManager pluginEventManager;

    public OsgiBundleFactory(OsgiContainerManager osgi, PluginEventManager pluginEventManager)
    {
        Validate.notNull(osgi, "The osgi container is required");
        Validate.notNull(pluginEventManager, "The plugin event manager is required");
        this.osgi = osgi;
        this.pluginEventManager = pluginEventManager;
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        String pluginKey = null;
        InputStream manifestStream = pluginArtifact.getResourceAsStream("META-INF/MANIFEST.MF");

        try
        {
            if (manifestStream != null)
            {
                Manifest mf;
                try {
                    mf = new Manifest(manifestStream);
                } catch (IOException e) {
                    throw new PluginParseException("Unable to parse manifest", e);
                }
                String symName = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (symName != null)
                {
                    pluginKey = getPluginKey(mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME),
                        mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
                }
            }
            return pluginKey;
        }
        finally
        {
            IOUtils.closeQuietly(manifestStream);
        }
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Validate.notNull(deploymentUnit, "The deployment unit is required");
        return create(new JarPluginArtifact(deploymentUnit.getPath()), moduleDescriptorFactory);
    }
    /**
     * Deploys the plugin artifact
     * @param pluginArtifact the plugin artifact to deploy
     * @param moduleDescriptorFactory The factory for plugin modules
     * @return The instantiated and populated plugin
     * @throws PluginParseException If the descriptor cannot be parsed
     * @since 2.2.0
     */
    public Plugin create(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");

        File file = pluginArtifact.toFile();
        Bundle bundle;
        try
        {
            bundle = osgi.installBundle(file);
        } catch (OsgiContainerException ex)
        {
            return reportUnloadablePlugin(file, ex);
        }
        String key = getPluginKey(bundle.getSymbolicName(), (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        return new OsgiBundlePlugin(bundle, key, pluginEventManager);
    }

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }

    private String getPluginKey(String symbolicName, String version)
    {
        return symbolicName + "-" + version;
    }
}