package com.atlassian.plugin.osgi.factory.transform.stage;

import static com.atlassian.plugin.osgi.factory.transform.JarUtils.withJar;
import static com.google.common.collect.Iterables.transform;

import com.atlassian.plugin.osgi.factory.transform.JarUtils;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * Contains utility functions for use in TransformStage implementations.
 *
 * @since 2.6.0 
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
     */
    static Set<String> scanJarForItems(final JarStreamSupplier supplier, final Set<String> expectedItems, final Function<JarEntry, String> mapper)
    {
        final JarInputStream inputStream = supplier.get();
        final Set<String> matches = new HashSet<String>();

        JarEntry entry;
        try
        {
            while ((entry = inputStream.getNextJarEntry()) != null)
            {
                final String item = mapper.apply(entry);
                if ((item != null) && expectedItems.contains(item))
                {
                    matches.add(item);
                    // early exit opportunity
                    if (matches.size() == expectedItems.size())
                    {
                        break;
                    }
                }
            }
        }
        catch (final IOException ex)
        {
            throw new PluginTransformationException("Exception getting entries from JarInputStream: " + supplier, ex);
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
    static Set<String> scanInnerJars(final File pluginFile, final Set<String> innerJars, final Set<String> expectedClasses)
    {
        return withJar(pluginFile, new JarUtils.Extractor<Set<String>>()
        {
            public Set<String> get(final JarFile pluginJarFile)
            {
                // this keeps track of all the matches.
                final Set<String> matches = new HashSet<String>();

                // scan each inner jar.
                for (final String innerJar : innerJars)
                {
                    final JarStreamSupplier innerJarStream = new JarStreamSupplier("Plugin file: ", pluginFile, " inner jar: ", innerJar)
                    {
                        public JarInputStream get()
                        {
                            try
                            {
                                return new JarInputStream(pluginJarFile.getInputStream(pluginJarFile.getEntry(innerJar)));
                            }
                            catch (final IOException e)
                            {
                                throw new PluginTransformationException(
                                    "Cannot open innerJar stream for pluginFile: " + pluginFile + " innerJar: " + innerJar, e);
                            }
                        }
                    };
                    // read inner jar into JarInputStream.
                    final Set<String> innerMatches = scanJarForItems(innerJarStream, expectedClasses, JarEntryToClassName.INSTANCE);
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
        });
    }

    /**
     * Try to close the given streams in order.
     * Exit once one is closed.
     *
     * @param streams streams to be closed. The higher ones must come first.
     */
    static void closeNestedStreamQuietly(final InputStream... streams)
    {
        for (final InputStream stream : streams)
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

    static abstract class JarStreamSupplier implements Supplier<JarInputStream>
    {
        private final Object[] names;

        JarStreamSupplier(final Object... names)
        {
            this.names = names;
        }

        @Override
        public String toString()
        {
            final StringBuilder result = new StringBuilder();
            for (final Object name : names)
            {
                result.append(name);
            }
            return result.toString();
        }
    }

    static class JarFileStream extends JarStreamSupplier
    {
        private final File pluginFile;

        JarFileStream(final File pluginFile)
        {
            super("Plugin file: ", pluginFile);
            this.pluginFile = pluginFile;
        }

        public JarInputStream get()
        {
            try
            {
                return new JarInputStream(new FileInputStream(pluginFile));
            }
            catch (final IOException e)
            {
                throw new PluginTransformationException("Cannot open stream for pluginFile: " + pluginFile, e);
            }
        }
    }
}
