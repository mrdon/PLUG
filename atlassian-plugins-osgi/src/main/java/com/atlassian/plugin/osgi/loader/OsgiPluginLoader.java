package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginJar;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.loader.transform.DefaultPluginTransformer;
import com.atlassian.plugin.osgi.loader.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.loader.transform.PluginTransformer;
import com.atlassian.plugin.parsers.DescriptorParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Plugin loader that starts an OSGi container and loads plugins into it, wrapped as OSGi bundles.
 */
public class OsgiPluginLoader extends ClassLoadingPluginLoader
{
    private static final Log log = LogFactory.getLog(OsgiPluginLoader.class);

    private OsgiContainerManager osgi;
    private HostComponentProvider hostComponentProvider;
    private PluginTransformer pluginTransformer;
    private final String pluginDescriptorFileName;


    public OsgiPluginLoader(File pluginPath, String pluginDescriptorFileName, PluginFactory pluginFactory, OsgiContainerManager osgi,
                            HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory);
        this.hostComponentProvider = provider;
        pluginTransformer = new DefaultPluginTransformer();
        this.osgi = osgi;
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        setDescriptorParserFactory(new ComponentFilteringXmlDescriptorParserFactory());
    }

    public void setPluginTransformer(PluginTransformer trans)
    {
        this.pluginTransformer = trans;
    }

    /**
     * Forces all registered host components to be unregistered and the HostComponentProvider to be called to get a
     * new list of host components
     * @param provider The host component provider to reload from
     */
    public void reloadHostComponents(HostComponentProvider provider)
    {
        osgi.reloadHostComponents(provider);
    }

    @Override
    public void shutDown()
    {
        super.shutDown();
        osgi.stop();
    }

    @Override
    public synchronized Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
    {
        if (!osgi.isRunning())
        {
            osgi.start(hostComponentProvider);
        }
        return super.loadAllPlugins(moduleDescriptorFactory);
    }
    
    @Override
    protected Plugin createPlugin(DescriptorParser parser, DeploymentUnit unit, PluginClassLoader loader) {
        Plugin plugin;
        switch (parser.getPluginsVersion()) {
            case 2  : plugin = createOsgiPlugin(unit.getPath(), false);
                      plugin.setEnabled(true);
                      break;
            default : plugin = super.createPlugin(parser, unit, loader);
        }
        return plugin;
    }

    @Override
    protected Plugin handleNoDescriptor(DeploymentUnit deploymentUnit) throws PluginParseException
    {
        try
        {
            JarFile jar = new JarFile(deploymentUnit.getPath());
            Attributes attrs = jar.getManifest().getMainAttributes();
            String name = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
            if (name != null) {
                return createOsgiPlugin(deploymentUnit.getPath(), true);
            }
        } catch (IOException e)
        {
            throw new PluginParseException("Unable to load jar", e);
        }

        throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);
    }

    @Override
    public String canLoad(PluginJar pluginJar) throws PluginParseException
    {
        String key = super.canLoad(pluginJar);

        if (key != null)
        {
            return key;
        }

        // no traditional atlassian-plugins.xml found
        try
        {
            Manifest mf = new Manifest(pluginJar.getFile("META-INF/MANIFEST.MF"));
            return mf.getMainAttributes().getValue("Bundle-SymbolicName");
        }
        catch (IOException e)
        {
            throw new PluginParseException("Unable to parse manifest in " + pluginDescriptorFileName, e);
        }
    }

    Plugin createOsgiPlugin(File file, boolean bundle)
    {
        try
        {
            if (bundle)
                return new OsgiBundlePlugin(osgi.installBundle(file));
            else
            {
                File transformedFile = pluginTransformer.transform(file, osgi.getHostComponentRegistrations());
                return new OsgiPlugin(osgi.installBundle(transformedFile));
            }
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
