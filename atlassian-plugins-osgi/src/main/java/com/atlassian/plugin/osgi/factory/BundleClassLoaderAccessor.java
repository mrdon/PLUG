package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.util.resource.AlternativeResourceLoader;
import com.atlassian.plugin.util.resource.NoOpAlternativeResourceLoader;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.Validate;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Utility methods for accessing a bundle as if it was a classloader.
 */
class BundleClassLoaderAccessor
{
    static ClassLoader getClassLoader(final Bundle bundle, final AlternativeResourceLoader alternativeResourceLoader)
    {
        return new BundleClassLoader(bundle, alternativeResourceLoader);
    }

    static <T> Class<T> loadClass(final Bundle bundle, final String name, final Class<?> callingClass) throws ClassNotFoundException
    {
        Validate.notNull(bundle, "The bundle is required");
        @SuppressWarnings("unchecked")
        final Class<T> loadedClass = bundle.loadClass(name);
        return loadedClass;
    }

    ///CLOVER:OFF
    /**
     * Fake classloader that delegates to a bundle
     */
    private static class BundleClassLoader extends ClassLoader
    {
        private final Bundle bundle;
        private final AlternativeResourceLoader altResourceLoader;

        public BundleClassLoader(final Bundle bundle, AlternativeResourceLoader altResourceLoader)
        {
            Validate.notNull(bundle, "The bundle must not be null");
            if (altResourceLoader == null)
            {
                altResourceLoader = new NoOpAlternativeResourceLoader();
            }
            this.altResourceLoader = altResourceLoader;
            this.bundle = bundle;

        }

        @Override
        public Class<?> findClass(final String name) throws ClassNotFoundException
        {
            return bundle.loadClass(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration<URL> findResources(final String name) throws IOException
        {
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
            URL url = altResourceLoader.getResource(name);
            if (url == null)
            {
                url = bundle.getResource(name);
            }
            return url;
        }
    }

}
