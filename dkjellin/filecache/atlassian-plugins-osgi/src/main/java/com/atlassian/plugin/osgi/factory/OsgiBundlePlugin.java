package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginArtifactBackedPlugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.impl.AbstractPlugin;
import com.atlassian.plugin.osgi.util.BundleClassLoaderAccessor;
import com.atlassian.plugin.util.resource.AlternativeDirectoryResourceLoader;
import org.apache.commons.lang.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * Plugin that wraps an OSGi bundle that has no plugin descriptor.
 */
public class OsgiBundlePlugin extends AbstractPlugin implements PluginArtifactBackedPlugin
{

    private final Bundle bundle;
    private final Date dateLoaded;
    private final ClassLoader bundleClassLoader;
    private final SynchronousBundleListener bundleStopListener;
    private final PluginArtifact pluginArtifact;

    public OsgiBundlePlugin(final Bundle bundle, final String key, final PluginArtifact pluginArtifact, final PluginEventManager pluginEventManager)
    {
        this.pluginArtifact = pluginArtifact;
        bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        Validate.notNull(bundle);
        this.bundle = bundle;
        // TODO: this should be done at a higher level than this to support start and stop
        bundleStopListener = new SynchronousBundleListener()
        {
            public void bundleChanged(final BundleEvent bundleEvent)
            {
                if (bundleEvent.getBundle() == bundle)
                {
                    if (bundleEvent.getType() == BundleEvent.STOPPING)
                    {
                        setPluginState(PluginState.DISABLED);
                    }
                }
            }
        };
        PluginInformation pluginInformation = new PluginInformation();
        pluginInformation.setDescription((String) bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION));
        pluginInformation.setVersion((String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        pluginInformation.setVendorName((String) bundle.getHeaders().get(Constants.BUNDLE_VENDOR));
        
        dateLoaded = new Date();
        setPluginsVersion(2);
        setName((String) bundle.getHeaders().get(Constants.BUNDLE_NAME));
        setKey(key);
        setPluginInformation(pluginInformation);
        setSystemPlugin(false);
    }


    @Override
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

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException
    {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz);
    }

    public URL getResource(final String name)
    {
        return bundleClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(final String name)
    {
        return bundleClassLoader.getResourceAsStream(name);
    }

    @Override
    protected void uninstallInternal()
    {
        try
        {
            if (bundle.getState() != Bundle.UNINSTALLED)
            {
                bundle.uninstall();
            }
        }
        catch (final BundleException e)
        {
            throw new PluginException(e);
        }
    }

    @Override
    protected PluginState enableInternal()
    {
        try
        {
            bundle.start();
            bundle.getBundleContext().addBundleListener(bundleStopListener);
            return PluginState.ENABLED;
        }
        catch (final BundleException e)
        {
            throw new PluginException(e);
        }
    }

    @Override
    protected void disableInternal()
    {
        try
        {
            if (bundle.getState() == Bundle.ACTIVE)
            {
                bundle.stop();
            }
        }
        catch (final BundleException e)
        {
            throw new PluginException(e);
        }
    }

    public ClassLoader getClassLoader()
    {
        return bundleClassLoader;
    }

    public PluginArtifact getPluginArtifact()
    {
        return pluginArtifact;
    }
}
