package com.atlassian.plugin.osgi.loader;

import com.atlassian.plugin.*;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.*;
import java.net.URL;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Plugin that wraps an OSGi bundle that has no plugin descriptor.
 */
public class OsgiBundlePlugin extends AbstractPlugin implements StateAware
{

    private Bundle bundle;
    private PluginInformation pluginInformation;
    private Date dateLoaded;

    public OsgiBundlePlugin(Bundle bundle)
    {
        this.bundle = bundle;
        this.pluginInformation = new PluginInformation();
        pluginInformation.setDescription((String) bundle.getHeaders().get("Bundle-Description"));
        pluginInformation.setVersion((String) bundle.getHeaders().get("Bundle-Version"));
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
        return bundle.getSymbolicName();
    }

    public void setI18nNameKey(String i18nNameKey)
    {
        throw new UnsupportedOperationException("Not available");
    }

    public String getKey()
    {
        return bundle.getSymbolicName();
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
        return Collections.EMPTY_LIST;
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

    public boolean isEnabled()
    {
        return Bundle.ACTIVE == bundle.getState();
    }

    public void setEnabled(boolean enabled)
    {
        if (isEnabled() && !enabled)
        {
            try
            {
                bundle.stop();
            } catch (BundleException e)
            {
                throw new RuntimeException("Cannot stop plugin: "+getKey());
            }
        } else if (!isEnabled() && enabled) {
            try
            {
                bundle.start();
            } catch (BundleException e)
            {
                throw new RuntimeException("Cannot start plugin: "+getKey());
            }
        }
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

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz, callingClass);
    }


    public URL getResource(String name)
    {
        return BundleClassLoaderAccessor.getResource(bundle, name);
    }

    public InputStream getResourceAsStream(String name)
    {
        return BundleClassLoaderAccessor.getResourceAsStream(bundle, name);
    }

    public ClassLoader getClassLoader()
    {
        return BundleClassLoaderAccessor.getClassLoader(bundle);
    }


    public void close()
    {
        try
        {
            bundle.uninstall();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot uninstall bundle " + bundle.getSymbolicName());
        }
    }

    public List getResourceDescriptors()
    {
        return Collections.EMPTY_LIST;
    }

    public List getResourceDescriptors(String type)
    {
        return Collections.EMPTY_LIST;
    }

    public ResourceLocation getResourceLocation(String type, String name)
    {
        return null;
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        return null;
    }

    public void enabled()
    {
        try
        {
            bundle.start();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot start plugin: "+getKey());
        }
    }

    public void disabled()
    {
        try
        {
            bundle.stop();
        } catch (BundleException e)
        {
            throw new RuntimeException("Cannot stop plugin: "+getKey());
        }
    }
}
