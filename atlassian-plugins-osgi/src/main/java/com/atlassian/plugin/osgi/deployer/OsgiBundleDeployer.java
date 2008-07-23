package com.atlassian.plugin.osgi.deployer;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginFrameworkShutdownEvent;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.deployer.PluginDeployer;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.deployer.transform.DefaultPluginTransformer;
import com.atlassian.plugin.osgi.deployer.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.deployer.transform.PluginTransformer;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Plugin deployer that deploys OSGi bundles that don't contain XML descriptor files
 */
public class OsgiBundleDeployer implements PluginDeployer
{
    private static final Log log = LogFactory.getLog(OsgiBundleDeployer.class);

    private OsgiContainerManager osgi;

    public OsgiBundleDeployer(OsgiContainerManager osgi)
    {
        this.osgi = osgi;
    }

    public String canDeploy(PluginArtifact pluginArtifact) throws PluginParseException {
        String pluginKey = null;
        InputStream manifestStream = pluginArtifact.getFile("META-INF/MANIFEST.MF");
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

    public Plugin deploy(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
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