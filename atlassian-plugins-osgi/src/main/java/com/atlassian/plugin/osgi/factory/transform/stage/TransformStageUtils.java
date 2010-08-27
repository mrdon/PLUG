package com.atlassian.plugin.osgi.factory.transform.stage;

import static com.google.common.collect.Iterables.transform;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Contains utility functions for use in TransformStage implementations.
 */
final class TransformStageUtils
{
    /**
     * Not for instantiation.
     */
    private TransformStageUtils()
    {}

    /**
     * Calculate classes available in the plugin. This doesn't include classes in embedded jars.
     *
     * @param jarStream stream of the plugin jar, it cannot be null.
     * @return the set of class names available within the plugin, never null.
     *
     * @throws java.io.IOException if there's problem reading the jarStream.
     */
    static Set<String> extractPluginClasses(final InputStream jarStream) throws IOException
    {
        final Set<String> classes = Sets.newHashSet();
        ZipInputStream zin = null;

        try
        {
            zin = new ZipInputStream(jarStream);
            ZipEntry zipEntry;

            while ((zipEntry = zin.getNextEntry()) != null)
            {
                final String path = jarPathToClassName(zipEntry.getName());
                if (path != null)
                {
                    classes.add(path);
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(zin);
        }

        return Collections.unmodifiableSet(classes);
    }

    /**
     * Extracts package name from the given class name.
     *
     * @param fullClassName a valid class name.
     * @return package name.
     */
    static String getPackageName(final String fullClassName)
    {
        return PackageName.INSTANCE.apply(fullClassName);
    }

    /**
     * Extract package names from the given set of classes.
     *
     * @param classes set of classes, cannot be null.
     * @return a set of package names, can be empty but never null.
     */
    static Set<String> getPackageNames(final Iterable<String> classes)
    {
        return ImmutableSet.copyOf(transform(classes, PackageName.INSTANCE));
    }

    /**
     * Convert a jar path to class name.
     * such as "com/atlassian/Test.class" -> "com.atlassian.Test".
     *
     * @param jarPath the entry name inside jar.
     * @return class name, or null if the path is not a class file.
     */
    static String jarPathToClassName(final String jarPath)
    {
        if ((jarPath == null) || !jarPath.contains(".class"))
        {
            return null;
        }

        return jarPath.replaceAll("/", ".").substring(0, jarPath.length() - ".class".length());
    }

    /**
     * Class name -> package name transformer.
     */
    enum PackageName implements Function<String, String>
    {
        INSTANCE;

        public String apply(final String fullClassName)
        {
            // A valid java class name must have a dot in it.
            return fullClassName.substring(0, fullClassName.lastIndexOf("."));
        }
    }
}
