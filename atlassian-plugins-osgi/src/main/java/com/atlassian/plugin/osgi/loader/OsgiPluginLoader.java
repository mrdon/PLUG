package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.loaders.ClassLoadingPluginLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.loader.transform.PluginTransformer;
import com.atlassian.plugin.osgi.loader.transform.DefaultPluginTransformer;
import com.atlassian.plugin.osgi.loader.transform.PluginTransformationException;
import com.atlassian.plugin.*;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.parsers.DescriptorParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
    private List<String> jarExcludes = Collections.emptyList();
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "org.quartz", "org.quartz.*", "bucket.*", "net.sf.cglib", "net.sf.cglib.*", "net.sf.hibernate", "net.sf.hibernate.*", "com.octo.captcha.*", "com.opensymphony.*", "org.apache.*", "org.xml.*", "javax.*", "org.w3c.*");
    private List<String> packageExcludes = Collections.emptyList();
    private OsgiContainerManager osgi;
    private HostComponentProvider hostComponentProvider;
    private PluginTransformer pluginTransformer;
    private Map<String, String> packageVersions;
    private final String pluginDescriptorFileName;

    public OsgiPluginLoader(File pluginPath, File startBundlesPath, String pluginDescriptorFileName, PluginFactory pluginFactory, HostComponentProvider provider)
    {
        super(pluginPath, pluginDescriptorFileName, pluginFactory);
        osgi = new FelixOsgiContainerManager(startBundlesPath);
        this.hostComponentProvider = provider;
        pluginTransformer = new DefaultPluginTransformer();
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
            Collection<ExportPackage> exports = generateExports();
            osgi.start(exports, hostComponentProvider);
        }
        return super.loadAllPlugins(moduleDescriptorFactory);
    }
    
    Collection<ExportPackage> generateExports()
    {
        Collection<ExportPackage> exports = new PackageScanner()
           .select(
               jars(
                       include((String[]) jarIncludes.toArray(new String[jarIncludes.size()])),
                       exclude((String[]) jarExcludes.toArray(new String[jarExcludes.size()]))),
               packages(
                       include((String[]) packageIncludes.toArray(new String[packageIncludes.size()])),
                       exclude((String[]) packageExcludes.toArray(new String[packageExcludes.size()])))
           )
           .withMappings(packageVersions)
           .scan();
        return exports;
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

    /**
     * Sets the jars to include and exclude from scanning
     * @param includes A list of jar patterns to include
     * @param excludes A list of jar patterns to exclude
     */
    public void setJarPatterns(List<String> includes, List<String> excludes) {
        this.jarIncludes = includes;
        this.jarExcludes = excludes;
    }

    /**
     * Sets the packages to include and exclude
     * @param includes A list of patterns to include
     * @param excludes A list of patterns to exclude
     */
    public void setPackagePatterns(List<String> includes, List<String> excludes) {
        this.packageIncludes = includes;
        this.packageExcludes = excludes;
    }

    /**
     * Maps discovered packages to specific versions by overriding autodiscovered versions
     * @param packageToVersions A map of package patterns to version strings
     */
    public void setPackageVersions(Map<String,String> packageToVersions)
    {
        this.packageVersions = packageToVersions;
    }

    void setOsgiContainerManager(OsgiContainerManager mgr)
    {
        this.osgi = mgr;
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
