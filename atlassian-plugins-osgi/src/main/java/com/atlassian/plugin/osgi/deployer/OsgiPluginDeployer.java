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
 * Plugin loader that starts an OSGi container and loads plugins into it, wrapped as OSGi bundles.
 */
public class OsgiPluginDeployer implements PluginDeployer
{
    private static final Log log = LogFactory.getLog(OsgiPluginDeployer.class);

    private OsgiContainerManager osgi;
    private PluginTransformer pluginTransformer;
    private final String pluginDescriptorFileName;
    private DescriptorParserFactory descriptorParserFactory;

    public OsgiPluginDeployer(String pluginDescriptorFileName, OsgiContainerManager osgi)
    {
        pluginTransformer = new DefaultPluginTransformer();
        this.osgi = osgi;
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new ComponentFilteringXmlDescriptorParserFactory();
    }

    public void setPluginTransformer(PluginTransformer trans)
    {
        this.pluginTransformer = trans;
    }

    public String canDeploy(PluginArtifact pluginArtifact) throws PluginParseException {
        String pluginKey = null;
        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getFile(pluginDescriptorFileName);
        } catch (PluginParseException ex)
        {
            // no descriptor, no worries
        }

        if (descriptorStream != null)
        {
            final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
            if (descriptorParser.getPluginsVersion() == 2)
                pluginKey = descriptorParser.getKey();
        }
        return pluginKey;
    }

    public Plugin deploy(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        ClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[]{deploymentUnit.getPath().toURL()}, null);
        } catch (MalformedURLException e) {
            throw new PluginParseException("Invalid plugin file", e);
        }

        if (loader.getResource(pluginDescriptorFileName) == null)
            throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);

        try
        {
            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
            plugin = parser.configurePlugin(moduleDescriptorFactory, createOsgiPlugin(deploymentUnit.getPath()));
        }
        finally
        {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }

    Plugin createOsgiPlugin(File file)
    {
        try
        {
            File transformedFile = pluginTransformer.transform(file, osgi.getHostComponentRegistrations());
            return new OsgiPlugin(osgi.installBundle(transformedFile));
        } catch (OsgiContainerException e)
        {
            return reportUnloadablePlugin(file, e);
        } catch (PluginTransformationException ex)
        {
            return reportUnloadablePlugin(file, ex);
        }
    }

    private Plugin reportUnloadablePlugin(File file, Exception e)
    {
        log.error("Unable to load plugin: "+file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: "+e.getMessage());
        return plugin;
    }
}
