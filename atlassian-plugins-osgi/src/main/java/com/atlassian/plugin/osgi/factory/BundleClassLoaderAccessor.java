package com.atlassian.plugin.osgi.factory;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Utility methods for accessing a bundle as if it was a classloader.
 */
class BundleClassLoaderAccessor
{
    private static final Logger log = Logger.getLogger(BundleClassLoaderAccessor.class);

    static ClassLoader getClassLoader(final Bundle bundle)
    {
        return new BundleClassLoader(bundle);
    }

    static <T> Class<T> loadClass(final Bundle bundle, final String name, final Class<?> callingClass) throws ClassNotFoundException
    {
        Validate.notNull(bundle, "The bundle is required");
        @SuppressWarnings("unchecked")
        final Class<T> loadedClass = bundle.loadClass(name);
        return loadedClass;
    }

    static URL getResource(final Bundle bundle, final String name)
    {
        Validate.notNull(bundle, "The bundle is required");
        return bundle.getResource(name);
    }

    static InputStream getResourceAsStream(final Bundle bundle, final String name)
    {
        Validate.notNull(bundle, "The bundle is required");
        final URL url = getResource(bundle, name);
        if (url != null)
        {
            try
            {
                return url.openStream();
            }
            catch (final IOException e)
            {
                log.debug("Unable to load resource from bundle: " + bundle.getSymbolicName(), e);
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
        private final Bundle bundle;

        public BundleClassLoader(final Bundle bundle)
        {
            Validate.notNull(bundle, "The bundle must not be null");
            this.bundle = bundle;
        }

        @Override
        public Class<?> findClass(final String name) throws ClassNotFoundException
        {
            return bundle.loadClass(name);
        }

        @Override
        public Enumeration<URL> findResources(final String name) throws IOException
        {
            @SuppressWarnings("unchecked")
            Enumeration<URL> e = bundle.getResources(name);

            // For some reason, getResources() sometimes returns nothing, yet getResource() will return one.  This code
            // handles that strange case
            if (!e.hasMoreElements())
            {
                final URL resource = findResource(name);
                if (resource != null)
                {
                    e = new IteratorEnumeration(Arrays.asList(resource).iterator());
                }
            }
            return e;
        }

        @Override
        public URL findResource(final String name)
        {
            return bundle.getResource(name);
        }
    }

}
