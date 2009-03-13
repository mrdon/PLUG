package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.*;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.util.resource.AlternativeDirectoryResourceLoader;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.*;
import java.net.URL;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.BundleException;

/**
 * Plugin that wraps an OSGi bundle that has no plugin descriptor.
 */
public class OsgiBundlePlugin extends AbstractPlugin
{

    private final Bundle bundle;
    private final PluginInformation pluginInformation;
    private final Date dateLoaded;
    private final String key;
    private final ClassLoader bundleClassLoader;

    public OsgiBundlePlugin(Bundle bundle, String key, PluginEventManager pluginEventManager)
    {
        this.bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        this.bundle = bundle;
        this.pluginInformation = new PluginInformation();
        pluginInformation.setDescription((String) bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION));
        pluginInformation.setVersion((String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        this.key = key;
        this.dateLoaded = new Date();
    }
    

    public int getPluginsVersion()
    {
        return 2;
    }

    public void setPluginsVersion(int version)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public String getName()
    {
        return (String) bundle.getHeaders().get("Bundle-Name");
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public String getI18nNameKey()
    {
        return key;
    }

    public void setI18nNameKey(String i18nNameKey)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String aPackage)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public void addModuleDescriptor(ModuleDescriptor moduleDescriptor)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public Collection getModuleDescriptors()
    {
        return Collections.emptyList();
    }

    public ModuleDescriptor getModuleDescriptor(String key)
    {
        return null;
    }

    public List getModuleDescriptorsByModuleClass(Class aClass)
    {
        return null;
    }

    public boolean isEnabledByDefault()
    {
        return true;
    }

    public void setEnabledByDefault(boolean enabledByDefault)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public PluginInformation getPluginInformation()
    {
        return pluginInformation;
    }

    public void setPluginInformation(PluginInformation pluginInformation)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public void setResources(Resourced resources)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public boolean isSystemPlugin()
    {
        return false;
    }

    public boolean containsSystemModule()
    {
        return false;
    }

    public void setSystemPlugin(boolean system)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public boolean isBundledPlugin()
    {
        return false;
    }

    public Date getDateLoaded()
    {
        return dateLoaded;
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public boolean isDeleteable()
    {
        return true;
    }

    public boolean isDynamicallyLoaded()
    {
        return true;
    }


    public List getResourceDescriptors()
    {
        return Collections.emptyList();
    }

    public List getResourceDescriptors(String type)
    {
        return Collections.emptyList();
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return null;
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return null;
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz, callingClass);
    }

    public URL getResource(final String name)
    {
        return bundleClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(final String name)
    {
        return bundleClassLoader.getResourceAsStream(name);
    }

    public void install()
    {

    }
    
    public void uninstall()
    {
        try
        {
            bundle.uninstall();
        }
        catch (BundleException e)
        {
            throw new PluginException(e);
        }
        super.uninstall();
    }

    @Override
    public void enable()
    {
        try
        {
            bundle.start();
        }
        catch (BundleException e)
        {
            throw new PluginException(e);
        }
        super.enable();
    }

    @Override
    public void disable()
    {
        try
        {
            bundle.stop();
        }
        catch (BundleException e)
        {
            throw new PluginException(e);
        }
        super.disable();
    }

    public ClassLoader getClassLoader()
    {
        return bundleClassLoader;
    }

}
