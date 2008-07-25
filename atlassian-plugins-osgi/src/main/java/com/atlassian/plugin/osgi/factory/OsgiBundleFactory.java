package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.Validate;
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

    public OsgiBundleFactory(OsgiContainerManager osgi)
    {
        Validate.notNull(osgi, "The osgi container is required");
        this.osgi = osgi;
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        String pluginKey = null;
        InputStream manifestStream = pluginArtifact.getResourceAsStream("META-INF/MANIFEST.MF");
        if (manifestStream != null)
        {
            Manifest mf;
            try {
                mf = new Manifest(manifestStream);
            } catch (IOException e) {
                throw new PluginParseException("Unable to parse manifest", e);
            }
            pluginKey = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        }
        return pluginKey;
    }

    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Validate.notNull(deploymentUnit, "The plugin deployment unit is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");
        
        Bundle bundle;
        try
        {
            bundle = osgi.installBundle(deploymentUnit.getPath());
        } catch (OsgiContainerException ex)
        {
            return reportUnloadablePlugin(deploymentUnit.getPath(), ex);
        }
        return new OsgiBundlePlugin(bundle);
    }

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }
}