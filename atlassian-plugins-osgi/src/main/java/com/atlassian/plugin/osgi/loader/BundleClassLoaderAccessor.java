package com.atlassian.plugin.osgi.loader;

import org.osgi.framework.Bundle;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for accessing a bundle as if it was a classloader.
 */
class BundleClassLoaderAccessor
{
    private static final Logger log = Logger.getLogger(BundleClassLoaderAccessor.class);

    static ClassLoader getClassLoader(Bundle bundle) {
        return new BundleClassLoader(bundle);
    }

    static Class loadClass(Bundle bundle, String name, Class callingClass) throws ClassNotFoundException
    {
        return bundle.loadClass(name);
    }

    static URL getResource(Bundle bundle, String name)
    {
        return bundle.getResource(name);
    }

    static InputStream getResourceAsStream(Bundle bundle, String name)
    {
        URL url = getResource(bundle, name);
        if (url != null) {
            try
            {
                return url.openStream();
            } catch (IOException e)
            {
                log.debug("Unable to load resource from bundle: "+bundle.getSymbolicName(), e);
            }
        }

        return null;
    }

    ///CLOVER:OFF
    /**
     * Fake classloader that delegates to a bundle
     */
    private static class BundleClassLoader extends ClassLoader
    {
        private Bundle bundle;

        public BundleClassLoader(Bundle bundle)
        {
            this.bundle = bundle;
        }

        @Override
        public Class findClass(String name) throws ClassNotFoundException
        {
            return bundle.loadClass(name);
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
    }


}
