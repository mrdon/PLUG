package com.atlassian.plugin.osgi.factory;

import org.osgi.framework.Bundle;
import org.apache.log4j.Logger;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.Validate;

import java.net.URL;
import java.util.Enumeration;
import java.util.Arrays;
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
        Validate.notNull(bundle, "The bundle is required");
        return bundle.loadClass(name);
    }

    static URL getResource(Bundle bundle, String name)
    {
        Validate.notNull(bundle, "The bundle is required");
        return bundle.getResource(name);
    }

    static InputStream getResourceAsStream(Bundle bundle, String name)
    {
        Validate.notNull(bundle, "The bundle is required");
        URL url = getResource(bundle, name);
        if (url != null) {
            try
            {
                return url.openStream();
            }
            catch (IOException e)
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
            Validate.notNull(bundle, "The bundle must not be null");
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
            Enumeration<URL> e = bundle.getResources(name);

            // For some reason, getResources() sometimes returns nothing, yet getResource() will return one.  This code
            // handles that strange case
            if (!e.hasMoreElements())
            {
                URL resource = findResource(name);
                if (resource != null)
                    e = new IteratorEnumeration(Arrays.asList(resource).iterator());
            }
            return e;
        }

        @Override
        public URL findResource(String name)
        {
            return bundle.getResource(name);
        }
    }


}
