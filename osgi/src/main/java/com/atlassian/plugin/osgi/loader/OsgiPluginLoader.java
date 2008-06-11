package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.parsers.DescriptorParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Attributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.*;
import static org.twdata.pkgscanner.PackageScanner.jars;
import static org.twdata.pkgscanner.PackageScanner.packages;
import static org.twdata.pkgscanner.PackageScanner.include;
import static org.twdata.pkgscanner.PackageScanner.exclude;
import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;

/**
 * Plugin loader that starts an OSGi container and loads plugins into it, wrapped as OSGi bundles.
 */
public class OsgiPluginLoader extends ClassLoadingPluginLoader
{
    private static final Log log = LogFactory.getLog(OsgiPluginLoader.class);

    private List<String> jarIncludes = Arrays.asList("*.jar");
    private List<String> jarExcludes = Collections.EMPTY_LIST;
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "org.apache.*", "org.xml.*", "javax.*", "org.w3c.*");
    private List<String> packageExcludes = Collections.EMPTY_LIST;
    private OsgiContainerManager osgi;
    private HostComponentProvider hostComponentProvider;

    public OsgiPluginLoader(File pluginPath, File startBundlesPath, PluginFactory pluginFactory, HostComponentProvider provider)
    {
        super(pluginPath, pluginFactory);
        osgi = new FelixOsgiContainerManager(startBundlesPath);
        this.hostComponentProvider = provider;
    }

    public OsgiPluginLoader(File pluginPath, File startBundlesPath, String pluginDescriptorFileName, PluginFactory pluginFactory, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory);
        osgi = new FelixOsgiContainerManager(startBundlesPath);
        this.hostComponentProvider = provider;
    }

    /**
     * Forces all registered host components to be unregistered and the HostComponentProvider to be called to get a
     * new list of host components
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
            Collection<ExportPackage> exports = new PackageScanner()
               .select(
                   jars(
                           include((String[]) jarIncludes.toArray(new String[0])),
                           exclude((String[]) jarExcludes.toArray(new String[0]))),
                   packages(
                           include((String[]) packageIncludes.toArray(new String[0])),
                           exclude((String[]) packageExcludes.toArray(new String[0]))))
               .scan();
            osgi.start(exports, hostComponentProvider);
        }
        return super.loadAllPlugins(moduleDescriptorFactory);
    }

    @Override
    protected Plugin createPlugin(DescriptorParser parser, DeploymentUnit unit, PluginClassLoader loader) {
        Plugin plugin;
        switch (parser.getPluginsVersion()) {
            case 2  : plugin = createOsgiPlugin(unit.getPath(), false);
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

    public void setJarPatterns(List<String> includes, List<String> excludes) {
        this.jarIncludes = includes;
        this.jarExcludes = excludes;
    }

    public void setPackagePatterns(List<String> includes, List<String> excludes) {
        this.packageIncludes = includes;
        this.packageExcludes = excludes;
    }

    private Plugin createOsgiPlugin(File file, boolean bundle)
    {
        try
        {
            if (bundle)
                return new OsgiBundlePlugin(osgi.installBundle(file));
            else
                return new OsgiPlugin(osgi.installBundle(file));
        } catch (OsgiContainerException e)
        {
            log.error("Unable to load plugin: "+file, e);
            
            UnloadablePlugin plugin = new UnloadablePlugin();
            plugin.setErrorText("Unable to load plugin: "+e.getMessage());
            return plugin;
        }
    }

}
