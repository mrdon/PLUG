package com.atlassian.plugin.osgi.factory.transform.stage;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Contains utility functions for use in TransformStage implementations.
 */
public final class TransformStageUtils
{
    /**
     * Not for instantiation.
     */
    private TransformStageUtils()
    {
    }

    /**
     * Calculate classes available in the plugin. This doesn't include classes in embedded jars.
     *
     * @param jarStream stream of the plugin jar, it cannot be null.
     * @return the set of class names available within the plugin, never null.
     *
     * @throws java.io.IOException if there's problem reading the jarStream.
     */
    static Set<String> extractPluginClasses(InputStream jarStream) throws IOException
    {
        Set<String> classes = new HashSet<String>();
        ZipInputStream zin = null;

        try
        {
            zin = new ZipInputStream(jarStream);
            ZipEntry zipEntry;

            while ((zipEntry = zin.getNextEntry()) != null)
            {
                String path = zipEntry.getName();
                if (path.endsWith(".class"))
                {
                    // truncate the final '.class' and convert all '/' to '.'.
                    classes.add(path.substring(0, path.length() - ".class".length()).replaceAll("/", "."));
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
    static String getPackageName(String fullClassName)
    {
        // A valid java class name must have a dot in it.
        return fullClassName.substring(0, fullClassName.lastIndexOf("."));
    }

    /**
     * Extract package names from the given set of classes.
     *
     * @param classes set of classes, cannot be null.
     * @return a set of package names, can be empty but never null.
     */
    static Set<String> getPackageNames(final Set<String> classes)
    {
        final Set<String> packages = new HashSet<String>();

        for(String clazz:classes)
        {
            packages.add(getPackageName(clazz));
        }

        return Collections.unmodifiableSet(packages);
    }

    /**
     * Convert a jar path to class name.
     * such as "com/atlassian/Test.class" -> "com.atlassian.Test".
     *
     * @param jarPath the entry name inside jar.
     * @return class name, or null if the path is not a class file.
     */
    static String jarPathToClassName(String jarPath)
    {
        if (jarPath == null || !jarPath.contains(".class"))
        {
            return null;
        }

        return jarPath.replaceAll("/", ".").substring(0, jarPath.length() - ".class".length());
    }
}
