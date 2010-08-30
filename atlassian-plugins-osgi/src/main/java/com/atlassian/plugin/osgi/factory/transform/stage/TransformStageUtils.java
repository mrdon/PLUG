package com.atlassian.plugin.osgi.factory.transform.stage;

import static com.google.common.collect.Iterables.transform;

import com.atlassian.plugin.osgi.factory.transform.JarUtils;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipFile;

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
     * Scan entries in jar for expectedItems.
     * It exits early once all the expected items are satisfied.
     *
     * @param inputStream the input jar entry stream, cannot be null. Caller is responsible for closing it.
     * @param expectedItems items expected from the stream, cannot be null.
     * @param mapper function which maps JarEntry to item which then can be matched against expectedItems, cannot be null.
     *
     * @return the set of matched items.
     *
     * @throws java.io.IOException IOException can be thrown if there is error while reading the stream.
     */
    static Set<String> scanJarForItems(final JarInputStream inputStream,
                                       final Set<String> expectedItems,
                                       final Function<JarEntry, String> mapper) throws IOException
    {
        final Set<String> matches = new HashSet<String>();
        JarEntry entry;

        while((entry=inputStream.getNextJarEntry()) != null)
        {
            final String item = mapper.apply(entry);
            if (item != null && expectedItems.contains(item))
            {
                matches.add(item);
                // early exit opportunity
                if (matches.size() == expectedItems.size())
                {
                    break;
                }
            }
        }

        return Collections.unmodifiableSet(matches);
    }

    /**
     * Scan inner jars for expected classes.
     * Early exit once all the required classes are satisfied.
     *
     * @param pluginFile the plugin jar file, cannot be null.
     * @param innerJars the inner jars to look at, never null. This is because there can be inner jars that we're not interested in the plugin.
     * @param expectedClasses the classes that we expect to find, never null.
     *
     * @return the set of classes matched, never null.
     */
    static Set<String> scanInnerJars(final File pluginFile,
                                     final Set<String> innerJars,
                                     final Set<String> expectedClasses)
                                                    throws IOException
    {
        // this keeps track of all the matches.
        final Set<String> matches = new HashSet<String>();

        JarFile pluginJarFile = null;
        try
        {
            // open the plugin jar file for reading.
            pluginJarFile = new JarFile(pluginFile, false, ZipFile.OPEN_READ);

            // scan each inner jar.
            for(String innerJar:innerJars)
            {
                Set<String> innerMatches;
                InputStream innerStream = null;
                JarInputStream innerJarStream = null;
                try
                {
                    // read inner jar into JarInputStream.
                    innerStream = pluginJarFile.getInputStream(pluginJarFile.getEntry(innerJar));
                    innerJarStream = new JarInputStream(innerStream);
                    innerMatches = TransformStageUtils.scanJarForItems(innerJarStream, expectedClasses, TransformStageUtils.JarEntryToClassName.INSTANCE);
                }
                catch (IOException ioe)
                {
                    IOException newException = new IOException("Error reading inner jar:" + innerJar);
                    newException.initCause(ioe);
                    throw newException;
                }
                finally
                {
                    closeNestedStreamQuietly(innerJarStream, innerStream);
                }

                // recalculate the matches.
                matches.addAll(innerMatches);

                // early exit.
                if (matches.size() == expectedClasses.size())
                {
                    break;
                }
            }

            return Collections.unmodifiableSet(matches);
        }
        catch (IOException ioe)
        {
            IOException newException = new IOException("Error reading jar:" + pluginFile.getName());
            newException.initCause(ioe);
            throw newException;
        }
        finally
        {
            JarUtils.closeQuietly(pluginJarFile);
        }
    }

    /**
     * Try to close the given streams in order.
     * Exit once one is closed.
     *
     * @param streams streams to be closed. The higher ones must come first.
     */
    static void closeNestedStreamQuietly(InputStream... streams)
    {
        for(InputStream stream:streams)
        {
            if (stream != null)
            {
                IOUtils.closeQuietly(stream);
                break;
            }
        }
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

    /**
     * Maps jarEntry -> class name.
     */
    enum JarEntryToClassName implements Function<JarEntry, String>
    {
        INSTANCE;

        public String apply(final JarEntry entry)
        {
            final String jarPath = entry.getName();
            if ((jarPath == null) || !jarPath.contains(".class"))
            {
                return null;
            }

            return jarPath.replaceAll("/", ".").substring(0, jarPath.length() - ".class".length());
        }
    }
}
