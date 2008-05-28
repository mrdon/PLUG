package com.atlassian.plugin.loaders.osgi;

import com.atlassian.plugin.impl.AbstractPlugin;
import java.util.Enumeration;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

import org.osgi.framework.Bundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: May 21, 2008
 * Time: 4:02:18 PM
 * This isn't the template you are looking for. It can go about its business.
 */
public class OsgiPlugin extends AbstractPlugin
{
    private Bundle bundle;
    private static final Log log = LogFactory.getLog(OsgiPlugin.class);
    private boolean deletable;
    private boolean bundled;

    public OsgiPlugin(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException
    {
        return bundle.loadClass(clazz);
    }

    public boolean isUninstallable()
    {
        return true;
    }

    public URL getResource(String name)
    {
        return bundle.getResource(name);
    }

    public InputStream getResourceAsStream(String name)
    {
        URL url = getResource(name);
        if (url != null) {
            try
            {
                return url.openStream();
            } catch (IOException e)
            {
                log.debug("Unable to load resource from plugin: "+getKey());
            }
        }

        return null;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return new ClassLoader() {
            @Override
            public Class findClass(String name) {
                return bundle.getClass();
            }

            @Override
            public Enumeration<URL> findResources(String name) throws IOException
            {
                return bundle.getResources(name);
            }

            @Override
            public URL findResource(String name)
            {
                return bundle.getResource(name);
            }
        };
    }

    /**
     * This plugin is dynamically loaded, so returns true.
     * @return true
     */
    public boolean isDynamicallyLoaded()
    {
        return true;
    }


    @Override
    public boolean isDeleteable()
    {
        return deletable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public boolean isBundledPlugin()
    {
        return bundled;
    }

    public void setBundled(boolean bundled)
    {
        this.bundled = bundled;
    }

    @Override
    public void close()
    {
        // Do nothing
    }
}
